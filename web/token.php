<?php

  // COMMANDS AND ARGUMENTS
  // start = start the generation process
  //  user = the user name
  //  password = the user's master password
  //  return = a tracking key
  // track = track the progress of the generation
  //  key = the tracking key
  //  return = the percent completion
  // clear = clear the cached file

  // Get the Command
  $cmd = $_REQUEST['cmd'];


  // START
  if ( $cmd == "start" ) {
    $user = $_REQUEST['username'];
    $password = $_REQUEST['password'];

    // Create tracking key
    $rand = generateRandomString(30);
    $key = hash('sha256', $user . $rand);
    echo $key . "\n";

    // File path
    $file = "/usr/share/nginx/pswd/tokens/" . $key;

    // Build the function call
    $call = "/usr/local/bin/pswdGenToken";
    $call = $call . " -u '" . $user . "'";
    $call = $call . " -p '" . $password . "'";
    $call = $call . " -f '" . $file . "'";
    $call = $call . " > /dev/null 2>&1 &";

    // Start the function
    $last_line = system($call, $retval);
  }



  // TRACK
  if ( $cmd == "track" ) {
    $key = $_REQUEST['key'];
    $key = preg_split('#\r?\n#', $key, 0)[0];
    $file = "/usr/share/nginx/pswd/tokens/" . $key;

    $last_line = system("cat '" . $file . "'", $retval);
  }


  // CLEAR
  if ( $cmd == "clear" ) {
    $key = $_REQUEST['key'];
    $key = preg_split('#\r?\n#', $key, 0)[0];
    $file = "/usr/share/nginx/pswd/tokens/" . $key;
    if (!unlink($file)) {
      echo ("false");
    }
    else {
      echo ("true");
    }
  }



  function generateRandomString($length = 10) {
    $characters = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    $randomString = '';
    for ($i = 0; $i < $length; $i++) {
      $randomString .= $characters[rand(0, strlen($characters) - 1)];
    }
    return $randomString;
  }

?>
