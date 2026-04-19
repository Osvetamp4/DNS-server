# DNS-server

$ORIGIN example.com.
$TTL 3600
@    IN  SOA   ns1.example.com. admin.example.com. (...)
@    IN  NS    ns1.example.com.
www  IN  A     192.168.1.10
mail IN  CNAME www.example.com.


# Conceptual layout of the object
record = {
    .rname:  "www.example.com.", # Expanded by $ORIGIN
    .rtype:  1,                  # Integer for 'A'
    .rclass: 1,                  # Integer for 'IN'
    .ttl:    3600,               
    .rdata:  <A record object with value '192.168.1.10'>
}


from dnslib import DNSRecord, DNSHeader, DNSQuestion, QTYPE

# Create a manual query for an A record
q = DNSRecord(q=DNSQuestion("google.com", QTYPE.A))

# Send it
self.send((root_ip, 60053), q, sock=temp_socket)