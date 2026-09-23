import runpy
import ssl

import certifi


def create_certifi_context(*args, **kwargs):
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    context.check_hostname = True
    context.verify_mode = ssl.CERT_REQUIRED
    context.load_verify_locations(cafile=certifi.where())
    return context


ssl.create_default_context = create_certifi_context
runpy.run_path("facetry.py", run_name="__main__")
