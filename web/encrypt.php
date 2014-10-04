<?php
  $USER_ID = $_REQUEST['user'];
  $USER_TOKEN = $_REQUEST['token'];

  include('cipher.php');

  $c = new Cipher($USER_ID);
  $encrypted = $c->encrypt($USER_TOKEN);

  echo $encrypted;
?>
