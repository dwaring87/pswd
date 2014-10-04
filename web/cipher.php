<?php
  class Cipher{
    private $securekey;

    function __construct($user_id) {
      $this->securekey = "A_PRIVATE_KEY" . "|" . $user_id;
    }

    function encrypt($input) {
      return base64_encode(mcrypt_encrypt(MCRYPT_RIJNDAEL_256, md5($securekey), $input, MCRYPT_MODE_CBC, md5(md5($securekey))));
    }

    function decrypt($input) {
      return rtrim(mcrypt_decrypt(MCRYPT_RIJNDAEL_256, md5($securekey), base64_decode($input), MCRYPT_MODE_CBC, md5(md5($securekey))), "\0");
    }
  }
?>
