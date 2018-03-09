#!/usr/bin/env python 

import sys
import time
import urllib
from urllib.parse import urlparse
import hmac
import hashlib
import base64

def get_auth_token(sb_name, eh_name, sas_name, sas_value):
    print(sb_name)
    print(eh_name)
    print(sas_name)
    print(sas_value)

    uri = urllib.parse.quote_plus("https://{}.servicebus.windows.net/{}" \
                                  .format(sb_name, eh_name))
    sas = sas_value.encode('utf-8')
    expiry = str(int(time.time() + 10000))
    string_to_sign = (uri + '\n' + expiry).encode('utf-8')
    signed_hmac_sha256 = hmac.HMAC(sas, string_to_sign, hashlib.sha256)
    signature = urllib.parse.quote(base64.b64encode(signed_hmac_sha256.digest()))
    return  {"sb_name": sb_name,
             "eh_name": eh_name,
             "token":'SharedAccessSignature sr={}&sig={}&se={}&skn={}' \
                     .format(uri, signature, expiry, sas_name)
            }

sb_name = sys.argv[1] 
eh_name = sys.argv[2]
sas_name = sys.argv[3]
sas_value = sys.argv[4]
print(get_auth_token(sb_name, eh_name, sas_name, sas_value))