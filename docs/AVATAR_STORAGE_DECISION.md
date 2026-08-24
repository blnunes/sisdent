# Avatar upload and storage decision

## Upload contract and processing

Avatar operations use authenticated GraphQL at `POST /graphql`. `uploadOwnAvatar` accepts
a typed `{ fileName, contentType, contentBase64 }` input, and `ownAvatar` returns the
normalized image as `{ contentType, contentBase64 }`; the Angular service immediately
converts both directions at the transport boundary. Spring GraphQL 2.0's HTTP handler
does not support the GraphQL multipart-request specification, so this explicit bounded
contract is approved until native multipart support is introduced. Request variables,
base64 content, and binary responses are never logged.

The service accepts JPEG (`image/jpeg`, `.jpg`, or `.jpeg`) and PNG (`image/png`,
`.png`) files up to 5 MiB. WebP is deliberately not advertised or accepted because
the standard Java image stack used by this application has no reliable WebP decoder.
The multipart envelope has a 6 MiB transport allowance so a valid 5 MiB file is not
rejected merely because of multipart boundaries; the application still enforces the
5 MiB file limit.

Input dimensions are checked before pixel decoding and are limited to 4096×4096.
For JPEGs, the processor applies the EXIF orientation tag emitted by phone cameras
before centre-cropping. The result is a correctly oriented, square 256×256 avatar,
which intentionally crops the longer dimension rather than stretching the photo.
The stored derivative is a newly encoded, metadata-free PNG with content type
`image/png`; no original image or EXIF data is retained.

Known validation failures use RFC 9457 problem details and stable codes:
`ACCOUNT.AVATAR_EMPTY`, `ACCOUNT.AVATAR_TOO_LARGE`,
`ACCOUNT.AVATAR_INVALID_TYPE`, and `ACCOUNT.AVATAR_INVALID_IMAGE`. Malformed or
missing multipart parts use request-validation codes. Every response carries
`X-Correlation-ID`; safe backend logs record the code, HTTP status, operation, and
correlation ID, never image bytes, multipart bodies, credentials, or physical paths.

## Storage

H2 stores only avatar metadata (`avatar_key`, content type, and update time); it does
not store image binaries. This remains appropriate for development and tests.

The local `ProfileAvatarStorage` implementation is suitable only for a single runtime
with a persistent, writable volume configured through
`SISDENT_AVATAR_STORAGE_DIRECTORY`. It creates and verifies that directory on startup,
writes replacements atomically on the same filesystem, and removes temporary files.

For multiple instances, ephemeral containers, or deployments without a shared
persistent volume, retain the `ProfileAvatarStorage` abstraction and provide an
object-storage implementation (S3/MinIO/Azure Blob according to the deployed
infrastructure). Keep binary objects in object storage and metadata in the relational
database; do not introduce H2 BLOBs as a filesystem workaround.

## Future storage optimization

The current 256×256 PNG output is lossless. It is predictable and preserves alpha,
but photographs may be smaller when encoded as JPEG. A future optimization may encode
opaque avatars as JPEG using a documented, configurable quality (for example 82–85)
and retain PNG when transparency is required. This must be a backend processing
decision so every client receives the same result; it does not require an Angular image
library. Such a change must update the generated key/content type, response tests, and
storage migration/cleanup behaviour before release.
