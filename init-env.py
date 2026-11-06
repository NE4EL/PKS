#!/usr/bin/env python3
"""Create local secrets once; never overwrite an existing environment."""
from pathlib import Path
import base64, os, secrets
path = Path(__file__).resolve().parent / '.env'
text = path.with_name('.env.example').read_text()
text = text.replace('replace_with_random_database_password', secrets.token_hex(24))
text = text.replace('replace_with_base64_encoded_32_random_bytes', base64.b64encode(secrets.token_bytes(48)).decode())
text = text.replace('replace_with_strong_temporary_password', 'Tmp1!' + secrets.token_hex(16))
try:
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
except FileExistsError:
    raise SystemExit('.env already exists; left unchanged')
with os.fdopen(fd, 'w') as stream:
    stream.write(text)
print('Created .env. Initial admin credentials are in that local file.')
