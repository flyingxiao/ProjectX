#!/usr/bin/python
#Inputfile:     flist
#Put the full path of zonefile which need to be analyzed on the flist
#Outputfile:    domain          :domain list
#               ns              :NS list
#               domainAndNs     :<domain,<nslist>?
import re
import os
import glob
from collections import defaultdict
import sys
import time
import tempfile

#Change cwd, so this script can be invoked using absolute path.
os.chdir(os.path.dirname(os.path.abspath(sys.argv[0])))

d = ""
orig = ""
ns_list_temp = []
zone_file_list = []

ns_2_domain = defaultdict(set)
domain_2_ns = defaultdict(set)

no_2_domain2ns_file = defaultdict(file)
no_2_ns2domain_file = defaultdict(file)

total_num = 0
root_ns_num = 0
tld_num = 0
sld_and_sub_num = 0

#RE pattern
orig_re = re.compile(r"^[$]ORIGIN\s+(\S+)")
nsnum_re = re.compile(r"^((\S*)\s+|\s*)(\d+\s+)?(IN\s+)?NS\s+(\S*)", re.IGNORECASE)
note_re = re.compile(r"^\s*;")

rr_re = re.compile(r"(IN)?(\s)+(RRSIG\s+)?(A|AAAA|AFSDB|APL|CERT|CNAME|DHCID|DLV|DNAME|DNSKEY|DS|HIP|IPSECKEY|KEY|KX|LOC|MX|RP|SIG|SOA|SPF|SRV|SSHFP|TKEY|TLSA|TSIG|TXT|NAPTR|NS|NSEC|NSEC3|NSEC3PARAM|PTR|RRSIG)(\s)+", re.IGNORECASE)

fout_num = open("count.s","w")
fout_d = open("domain.s","w")
fout_ns = open("ns.s","w")
fout_d_and_ns = open("domainAndNs.s","w")
fout_ns_and_d = open("nsAndDomain.s","w")
fout_ns_and_d_pair = open("nsAndDomainPair.s","w")
last_num = 0

split_num = 20
def get_tmp_file_prefix():
  #.time_pid
  return "." + str(time.time()) + "_" + str(os.getpid())

#Get tm file prefix.
tmp_file_prefix = get_tmp_file_prefix()

#Get zone file list first, if line in file represents a dir, only files in this dir whose name ends with .zone
# will be included.
for zone_file in  open("flist"):
  zone_file = zone_file.strip()

  if(os.path.isfile("" + zone_file) == True):
    zone_file_list.append(zone_file)
  elif(os.path.isdir("" + zone_file) == True):
    zone_file_list.extend(glob.glob(zone_file + "/*.zone"))
  else:continue

#Open tmp file for ns to domain cache.
#no_2_ns2domain_file = dict((no, open(tmp_file_prefix + "_ns2domain_" + str(no), "w+")) for no in range(0, split_num))
no_2_ns2domain_file = dict((no, tempfile.TemporaryFile(mode = "w+", dir = "./")) for no in range(0, split_num))

#Deal with every zone file
for zone_file_name in zone_file_list:
  #Open tmp file for domain to ns cache.
  #no_2_domain2ns_file = dict((no, open(tmp_file_prefix + "_domain2ns_" + str(no), "w+")) for no in range(0, split_num))
  no_2_domain2ns_file = dict((no, tempfile.TemporaryFile(mode = "w+", dir = "./")) for no in range(0, split_num))

  zone_file_name = zone_file_name.strip()

  #Try to get origin from file name first.
  orig = os.path.splitext(os.path.basename(zone_file_name))[0].lower()
  if orig == "root":
    orig = "."
  if (len(orig) > 0) and (orig[-1] != "."):
    orig += "."

  zone_name = orig

  #Process every line.
  for line in open(zone_file_name):
    line = line.replace("\t"," ")
    line = line.rstrip()

    if(line == ""):
      continue

    #Skip note line.
    note = note_re.search(line)
    if(note):
      continue

    #Replace @ with origin info.
    if(orig):
      line = line.replace("@",orig)
         
    #Check whether line is a origin line.
    ori = orig_re.search(line)
    if(ori):
      orig = ori.group(1)
      orig = orig.lower()
      #if(total_num):
      #  fout_num.write("The total number of " + orig + " is " + str(total_num-last_num) + "\n")
    else:
      #Check whether line is a rr line.
      r = rr_re.search(line)
      if(r == None):
        continue
      
      #Get zone name.
      if (r.group(3) == "SOA"):
        zone_name = orig

      #Get current domain name.
      d_temp = line.split(" ")[0]
      d_temp = d_temp.strip()                               
      if(d_temp != ""):
        if(d_temp[-1] == "."):
          dt = d_temp
        else:
          if(orig != "."):
            dt = d_temp + "." + orig
          else:
            dt = d_temp + "."
        dt = dt.lower()

        #When a new domain appears, old domain info should be flushed to tmp
        #files.
        if(dt != d):
          #Zone name (except for root ) info is not included.
          if(d != ""):
            if(ns_list_temp != []):
              if ((d != zone_name) or (d == ".")):
                #Cache to tmp file.
                domain2ns_file = no_2_domain2ns_file[abs(hash(d)) % (split_num)]
                domain2ns_file.write("%s"%d)

                for ns in ns_list_temp:
                  domain2ns_file.write("\t%s"%ns)
                  no_2_ns2domain_file[abs(hash(ns)) % (split_num)].write("%s\t%s\n"%(ns, d))
                domain2ns_file.write("\n")

              ns_list_temp = []                                               
          #Renew current domain name
          d = dt

      #Check whether line is a ns line.
      p = nsnum_re.search(line)
      if(p) and ((d != zone_name) or (d == ".")):
        total_num = total_num + 1

        ns = p.group(5)
        ns = ns.lower()
        if (ns[-1] != "."):
          if (orig != "."): 
            ns = ns + "." + orig
          else:
            ns = ns + orig
        ns_list_temp.append(ns)                                              

  if(ns_list_temp != []):
    if ((d != zone_name) or (d == ".")):
      domain2ns_file = no_2_domain2ns_file[abs(hash(d)) % (split_num)]
      domain2ns_file.write("%s"%d)

      for ns in ns_list_temp:
        domain2ns_file.write("\t%s"%ns)
        no_2_ns2domain_file[abs(hash(ns)) % (split_num)].write("%s\t%s\n"%(ns, d))

      domain2ns_file.write("\n")

    ns_list_temp = []                         

  #When a zone file have been processed, domain info can be processed now.
  for  no in range(0, split_num):
    domain_2_ns = defaultdict(set)
    domain2ns_file = no_2_domain2ns_file[no]
    domain2ns_file.seek(0)
    for line in domain2ns_file:
      line = line.rstrip()
      if(line == ""):
        continue
      fields = line.split("\t")
      domain_2_ns[fields[0]].update(fields[1:])

    for d in domain_2_ns:

      dot_count = d.count(".")
      if dot_count == 1:
        if len(d) == 1:
          root_ns_num = len(domain_2_ns[d])
        elif len(d) > 1:
          tld_num += 1
      elif dot_count > 1:
        sld_and_sub_num += 1

      dn = "www." + d
      fout_d.write("%s\n"%dn)
      fout_d_and_ns.write("%s\t%s\n"%(d, "\t".join(domain_2_ns[d])))

    domain2ns_file.close()

  print "Resolution process of " + zone_file_name + " is completed"

domain_2_ns = defaultdict(set)

#Get unique ns info from tmp file.
for  no in range(0, split_num):
  ns_2_domain = defaultdict(set)

  ns2domain_file = no_2_ns2domain_file[no]
  ns2domain_file.seek(0)
  for line in ns2domain_file:
    line = line.rstrip()
    if(line == ""):
      continue
    fout_ns_and_d_pair.write("%s\n"%(line))
    fields = line.split("\t")
    ns_2_domain[fields[0]].update(fields[1:])

  for ns in ns_2_domain:
    fout_ns.write("%s\n"%ns)
    fout_ns_and_d.write("%s\t%s\n"%(ns, "\t".join(ns_2_domain[ns])))

  ns2domain_file.close()

#fout_num.write("The total number of all  is " + str(total_num))
fout_num.write(str(root_ns_num) + "\t" + str(tld_num) + "\t" + str(sld_and_sub_num) + "\n")

fout_num.close()
fout_d.close()
fout_d_and_ns.close()
fout_ns_and_d_pair.close()
fout_ns.close()
fout_ns_and_d.close()

