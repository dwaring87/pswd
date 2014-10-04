<?php

  include('cipher.php');

  // GENERATE A PASSWORD
  // Arguments
  // -u : username
  // -p : master password
  // -t : token
  // -d : domain
  // -n : length
  // -c : uppercase letters
  // -s : symbols
  // -a : symbol characters
  // -y : k2 number of hashes

  // Get Arguments
  $username = $_REQUEST['u'];
  $password = $_REQUEST['p'];
  $encrypted_token = $_REQUEST['e'];
  $domain = $_REQUEST['d'];
  $length = $_REQUEST['n'];
  $caps = $_REQUEST['c'];
  $symbols = $_REQUEST['s'];
  $symbol_chars = $_REQUEST['a'];
  $k2 = $_REQUEST['y'];


  // Decrypt Token
  $c = new Cipher(hash('sha256', $username));
  $token = $c->decrypt($encrypted_token);


  // Build Function Call
  $call = "/usr/local/bin/pswd";

  // add username
  $call = $call . " -u '" . $username . "'";

  // add master password
  $call = $call . " -p '" . $password . "'";

  // add user token
  $call = $call . " -t '" . $token . "'";

  // add domain
  $domain = preg_replace('/\s+/', '', $domain);
  $domain = strtolower($domain);
  $call = $call . " -d '" . $domain . "'";

  // add length
  $call = $call . " -n " . $length;

  // add caps, if false (default is true)
  if ( $caps == "false" ) {
    $call = $call . " -c";
  }

  // add symbols, if false (default is true)
  if ( $symbols == "false" ) {
    $call = $call . " -s";
  }

  // add symbol characters (when true)
  else {
    $call = $call . " -a '" . $symbol_chars . "'";
  }

  $call = $call . " -y " . $k2;


  // Call the function
  $output = shell_exec($call);
  echo $output;

?>
