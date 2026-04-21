from dnslib import RR
from enum import Enum
from dnslib import DNSRecord, DNSHeader, RR, QTYPE, A, DNSQuestion

class RecordType(Enum):
    """DNS Record Type Numbers"""
    A = 1
    NS = 2
    CNAME = 5
    SOA = 6
    MX = 15
    TXT = 16
# Format: <name> <TTL> <Class> <Type> <Data>
# Note: CNAME is type 5, A is type 1.
zone_data = """
cname.6.    60  IN  CNAME  cname.7.
cname.1.    60  IN  CNAME  cname.2.
cname.3.    60  IN  CNAME  cname.4.
cname.4.    60  IN  CNAME  cname.5.
cname.2.    60  IN  CNAME  cname.3.
cname.5.    60  IN  CNAME  cname.6.
cname.7.    60  IN  CNAME  somewhere.else.
somewhere.else.    60  IN  A      1.2.3.4
"""

zone_data1 = """
somewhere.else.    60  IN  A      1.2.3.4
cname.7.    60  IN  CNAME  somewhere.else.
"""
# This returns a list of RR objects
test_records = RR.fromZone(zone_data1)
for i in test_records:
    print(i)

def get_record_by_name(target_record_name,records):
    for record in records:
        if record.rname == target_record_name:
            return record
    return None
    
def get_cname_chain_only(records):
    CNAME_record_exists = False
    for record in records:
        if record.rtype == RecordType.CNAME.value:
            CNAME_record_exists = True
            break
    if CNAME_record_exists == False: return []

    if len(records) == 1:
        return records

    #maps a record object A (A->B) like B : A
    CNAME_record_dictionary = dict()
    for record in records:
        if record.rtype == RecordType.CNAME.value:
            next_pointed_record = get_record_by_name(str(record.rdata),records)
            if next_pointed_record == None:
                continue
            CNAME_record_dictionary[str(next_pointed_record.rname)] = record
        
    
    current_key = list(CNAME_record_dictionary.keys())[0]
    while current_key in CNAME_record_dictionary:
        current_key = str(CNAME_record_dictionary[current_key].rname)
        #print(CNAME_record_dictionary[current_key])
        print("current key",current_key)
    

    
    current_CNAME_record = get_record_by_name(current_key,records)
    current_CNAME_rname = str(current_CNAME_record.rname)
    CNAME_chain_list = []
    while current_CNAME_record != None:
        CNAME_chain_list.append(current_CNAME_record)
        current_CNAME_rname = str(current_CNAME_record.rdata) #Gets the next name and puts it into current
        current_CNAME_record = get_record_by_name(current_CNAME_rname,records)
    

    return CNAME_chain_list

print(get_cname_chain_only(test_records))