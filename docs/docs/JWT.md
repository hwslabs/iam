# IAM JWT Token 

## Structure

The structure of a JWT token is as what is specified in [RFC7519](https://www.rfc-editor.org/rfc/rfc7519.html).


**Header:**

**kid** - This field contains the key-id which was used to sign the JWT token. This is further explained in the Signature section.

**zip** - This field contains the algorithm which was used to compress the body.
Further info in [Notes on Compression](JWT.md#notes-on-compression) section. 

```json5
{
  "kid": "aa5db89b-112b-4d58-918b-f7e485cf3e47",
  "alg": "ES256",
  "zip": "GZIP" // Indicates the algorithm used to compress the body
}
```

**Body:**

The **entitlements** claim contains all the permissions that the user has in
[**Casbin policy definition**](https://casbin.org/docs/en/syntax-for-models#policy-definition) format.

```json5
{
  "iss": "https://iam.hypto.com",
  "iat": 1647500446, //epoch seconds
  "exp": 1647500746, //epoch seconds
  "ver": "1.0",
  "usr": "hrn:N1nEvjKSCr::iam-user/admin", //user HRN
  "org": "N1nEvjKSCr", // Organization id
  "entitlements": "p, hrn:N1nEvjKSCr::iam-policy/admin, N1nEvjKSCr, hrn:N1nEvjKSCr:*, allow\n\ng, hrn:N1nEvjKSCr::iam-user/admin, hrn:N1nEvjKSCr::iam-policy/admin\n"
}
```

**Signature:**
IAM maintains private and public key pair to sign the JWT tokens which are rotated as a best practice.
Each key pair is identifiable using a key-id which is sent as part of JWT header. In the future, clients can use this
identifier to request the respective public key in order to verify the signature.

#### Notes on Compression

IAM uses the JWS compression feature provided by [jjwt library](https://github.com/jwtk/jjwt) to create and parse JWT tokens.
This feature might not be available in many other libraries as JWT specification only standardizes this feature for
JWEs (Encrypted JWTs) and not JWSs (Signed JWTs) as mentioned [here](https://github.com/jwtk/jjwt#compression).
But, this is used by IAM to increase the number of permissions that can be assigned to users. In the future,
IAM might use JWEs instead of JWTs to abide by specification.