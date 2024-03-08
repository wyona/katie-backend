# Private and public keys to generate JWTs

TODO: Regenerate key pair on a regular basis, whereas either invalidate tokens or keep previous public key to validate previously generated tokens

The private key "private_key_pkcs8.pem" is stored in a Base64 encoded PEM format (see https://en.wikipedia.org/wiki/Privacy-Enhanced_Mail)

Generate private and public keys https://gist.github.com/destan/b708d11bd4f403506d6d5bb5fe6a82c5

- openssl genrsa -out private_key.pem 512
- #openssl genrsa -out private_key.pem 4096
- openssl rsa -pubout -in private_key.pem -out public_key.pem
- openssl pkcs8 -topk8 -in private_key.pem -inform pem -out private_key_pkcs8.pem -outform pem -nocrypt
