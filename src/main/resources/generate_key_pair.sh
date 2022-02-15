#!/bin/bash

# Navigate to work dir
cd /tmp || exit 1

# Clean up
rm private_key.pem public_key.pem private_key.der public_key.der

#- Create a ES256 private key pem:
openssl ecparam -name prime256v1 -genkey -noout -out private_key.pem

#- Generate corresponding  public key pem:
openssl ec -in private_key.pem -pubout -out public_key.pem

#- Generate private_key.der corresponding to private_key.pem
openssl pkcs8 -topk8 -inform PEM -outform DER -in  private_key.pem -out  private_key.der -nocrypt

#- Generate public_key.der corresponding to private_key.pem
openssl ec -in private_key.pem -pubout -outform DER -out public_key.der