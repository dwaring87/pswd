<?php
  class Cipher{
    private $securekey;

    function __construct($user_id) {
      $this->securekey = "A_PRIVATE_KEY" . "|" . $user_id;
    }

    function encrypt($input) {
      return base64_encode(openssl_encrypt($input, "aes-256-cbc", md5($securekey), OPENSSL_RAW_DATA|OPENSSL_ZERO_PADDING, hex2bin(md5(md5($securekey)))));
    }

    function decrypt($input) {
      return openssl_decrypt(base64_decode($input), "aes-256-cbc", md5($securekey), OPENSSL_RAW_DATA|OPENSSL_ZERO_PADDING, hex2bin(md5(md5($securekey))));
    }
  }
?>
