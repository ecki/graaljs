/*
 * Copyright 2021 The OpenSSL Project Authors. All Rights Reserved.
 *
 * Licensed under the Apache License 2.0 (the "License").  You may not use
 * this file except in compliance with the License.  You can obtain a copy
 * in the file LICENSE in the source distribution or at
 * https://www.openssl.org/source/license.html
 */

#include <string.h>
#include <openssl/pkcs7.h>
#include <openssl/x509.h>
#include <openssl/x509v3.h>
#include <openssl/pem.h>
#include "internal/nelem.h"
#include "testutil.h"

#ifndef OPENSSL_NO_EC
static const unsigned char cert_der[] = {
    0x30, 0x82, 0x01, 0x51, 0x30, 0x81, 0xf7, 0xa0, 0x03, 0x02, 0x01, 0x02,
    0x02, 0x02, 0x03, 0x09, 0x30, 0x0a, 0x06, 0x08, 0x2a, 0x86, 0x48, 0xce,
    0x3d, 0x04, 0x03, 0x02, 0x30, 0x27, 0x31, 0x0b, 0x30, 0x09, 0x06, 0x03,
    0x55, 0x04, 0x06, 0x13, 0x02, 0x55, 0x53, 0x31, 0x18, 0x30, 0x16, 0x06,
    0x03, 0x55, 0x04, 0x03, 0x0c, 0x0f, 0x63, 0x72, 0x79, 0x70, 0x74, 0x6f,
    0x67, 0x72, 0x61, 0x70, 0x68, 0x79, 0x20, 0x43, 0x41, 0x30, 0x1e, 0x17,
    0x0d, 0x31, 0x37, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x30, 0x31, 0x30,
    0x30, 0x5a, 0x17, 0x0d, 0x33, 0x38, 0x31, 0x32, 0x33, 0x31, 0x30, 0x38,
    0x33, 0x30, 0x30, 0x30, 0x5a, 0x30, 0x27, 0x31, 0x0b, 0x30, 0x09, 0x06,
    0x03, 0x55, 0x04, 0x06, 0x13, 0x02, 0x55, 0x53, 0x31, 0x18, 0x30, 0x16,
    0x06, 0x03, 0x55, 0x04, 0x03, 0x0c, 0x0f, 0x63, 0x72, 0x79, 0x70, 0x74,
    0x6f, 0x67, 0x72, 0x61, 0x70, 0x68, 0x79, 0x20, 0x43, 0x41, 0x30, 0x59,
    0x30, 0x13, 0x06, 0x07, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02, 0x01, 0x06,
    0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00,
    0x04, 0x18, 0xff, 0xcf, 0xbb, 0xf9, 0x39, 0xb8, 0xf5, 0xdd, 0xc3, 0xee,
    0xc0, 0x40, 0x8b, 0x06, 0x75, 0x06, 0xab, 0x4f, 0xcd, 0xd8, 0x2c, 0x52,
    0x24, 0x4e, 0x1f, 0xe0, 0x10, 0x46, 0x67, 0xb5, 0x5f, 0x15, 0xb9, 0x62,
    0xbd, 0x3b, 0xcf, 0x0c, 0x6f, 0xbe, 0x1a, 0xf7, 0xb4, 0xa1, 0x0f, 0xb4,
    0xb9, 0xcb, 0x6e, 0x86, 0xb3, 0x50, 0xf9, 0x6c, 0x51, 0xbf, 0xc1, 0x82,
    0xd7, 0xbe, 0xc5, 0xf9, 0x05, 0xa3, 0x13, 0x30, 0x11, 0x30, 0x0f, 0x06,
    0x03, 0x55, 0x1d, 0x13, 0x01, 0x01, 0xff, 0x04, 0x05, 0x30, 0x03, 0x01,
    0x01, 0xff, 0x30, 0x0a, 0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x04,
    0x03, 0x02, 0x03, 0x49, 0x00, 0x30, 0x46, 0x02, 0x21, 0x00, 0xd1, 0x12,
    0xef, 0x8d, 0x97, 0x5a, 0x6e, 0xb8, 0xb6, 0x41, 0xa7, 0xcf, 0xc0, 0xe7,
    0xa4, 0x6e, 0xae, 0xda, 0x51, 0xe4, 0x64, 0x54, 0x2b, 0xde, 0x86, 0x95,
    0xbc, 0xf7, 0x1e, 0x9a, 0xf9, 0x5b, 0x02, 0x21, 0x00, 0xd1, 0x61, 0x86,
    0xce, 0x66, 0x31, 0xe4, 0x2f, 0x54, 0xbd, 0xf5, 0xc8, 0x2b, 0xb3, 0x44,
    0xce, 0x24, 0xf8, 0xa5, 0x0b, 0x72, 0x11, 0x21, 0x34, 0xb9, 0x15, 0x4a,
    0x5f, 0x0e, 0x27, 0x32, 0xa9
};

static int pkcs7_verify_test(void)
{
    int ret = 0;
    size_t i;
    BIO *msg_bio = NULL, *x509_bio = NULL, *bio = NULL;
    X509 *cert = NULL;
    X509_STORE *store = NULL;
    PKCS7 *p7 = NULL;
    const char *sig[] = {
        "MIME-Version: 1.0\nContent-Type: multipart/signed; protocol=\"application/x-pkcs7-signature\"; micalg=\"sha-256\"; boundary=\"----9B5319FF2E4428B17CD26B69294E7F31\"\n\n",
        "This is an S/MIME signed message\n\n------9B5319FF2E4428B17CD26B69294E7F31\n",
        "Content-Type: text/plain\r\n\r\nhello world\n------9B5319FF2E4428B17CD26B69294E7F31\n",
        "Content-Type: application/x-pkcs7-signature; name=\"smime.p7s\"\n",
        "Content-Transfer-Encoding: base64\nContent-Disposition: attachment; filename=\"smime.p7s\"\n\n",
        "MIIDEgYJKoZIhvcNAQcCoIIDAzCCAv8CAQExDzANBglghkgBZQMEAgEFADALBgkq\nhkiG9w0BBwGgggFVMIIBUTCB96ADAgECAgIDCTAKBggqhkjOPQQDAjAnMQswCQYD\nVQQGEwJVUzEYMBYGA1UEAwwPY3J5cHRvZ3JhcGh5IENBMB4XDTE3MDEwMTEyMDEw\nMFoXDTM4MTIzMTA4MzAwMFowJzELMAkGA1UEBhMCVVMxGDAWBgNVBAMMD2NyeXB0\nb2dyYXBoeSBDQTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABBj/z7v5Obj13cPu\nwECLBnUGq0/N2CxSJE4f4BBGZ7VfFblivTvPDG++Gve0oQ+0uctuhrNQ+WxRv8GC\n",
        "177F+QWjEzARMA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDSQAwRgIhANES\n742XWm64tkGnz8DnpG6u2lHkZFQr3oaVvPcemvlbAiEA0WGGzmYx5C9UvfXIK7NE\nziT4pQtyESE0uRVKXw4nMqkxggGBMIIBfQIBATAtMCcxCzAJBgNVBAYTAlVTMRgw\nFgYDVQQDDA9jcnlwdG9ncmFwaHkgQ0ECAgMJMA0GCWCGSAFlAwQCAQUAoIHkMBgG\nCSqGSIb3DQEJAzELBgkqhkiG9w0BBwEwHAYJKoZIhvcNAQkFMQ8XDTIxMDUyMDE4\nNTA0OVowLwYJKoZIhvcNAQkEMSIEIOdwMRgQrqcnmMYvag+BVvErcc6bwUXI94Ds\n",
        "QkiyIU9pMHkGCSqGSIb3DQEJDzFsMGowCwYJYIZIAWUDBAEqMAsGCWCGSAFlAwQB\nFjALBglghkgBZQMEAQIwCgYIKoZIhvcNAwcwDgYIKoZIhvcNAwICAgCAMA0GCCqG\nSIb3DQMCAgFAMAcGBSsOAwIHMA0GCCqGSIb3DQMCAgEoMAoGCCqGSM49BAMCBEcw\nRQIhANYMJku1fW9T1MIEcAyREArz9kXCY4tWck5Pt0xzrYhaAiBDSP6e43zj4YtI\nuvQW+Lzv+dNF8EPuhgoPNe17RuUSLw==\n\n------9B5319FF2E4428B17CD26B69294E7F31--\n\n"
    };
    const char *signed_data = "Content-Type: text/plain\r\n\r\nhello world";

    if (!TEST_ptr(bio = BIO_new(BIO_s_mem())))
        goto end;
    for  (i = 0; i < OSSL_NELEM(sig); ++i)
        BIO_puts(bio, sig[i]);

    ret = TEST_ptr(msg_bio = BIO_new_mem_buf(signed_data, strlen(signed_data)))
          && TEST_ptr(x509_bio = BIO_new_mem_buf(cert_der, sizeof(cert_der)))
          && TEST_ptr(cert = d2i_X509_bio(x509_bio, NULL))
          && TEST_int_eq(ERR_peek_error(), 0)
          && TEST_ptr(store = X509_STORE_new())
          && TEST_true(X509_STORE_add_cert(store, cert))
          && TEST_ptr(p7 = SMIME_read_PKCS7(bio, NULL))
          && TEST_int_eq(ERR_peek_error(), 0)
          && TEST_true(PKCS7_verify(p7, NULL, store, msg_bio, NULL, PKCS7_TEXT))
          && TEST_int_eq(ERR_peek_error(), 0);
end:
    X509_STORE_free(store);
    X509_free(cert);
    PKCS7_free(p7);
    BIO_free(msg_bio);
    BIO_free(x509_bio);
    BIO_free(bio);
    return ret;
}
#endif /* OPENSSL_NO_EC */

int setup_tests(void)
{
#ifndef OPENSSL_NO_EC
    ADD_TEST(pkcs7_verify_test);
#endif /* OPENSSL_NO_EC */
    return 1;
}
