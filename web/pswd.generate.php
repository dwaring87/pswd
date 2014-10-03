<?php

// Get the arguments
$caps = $_REQUEST['c'];
$domain = $_REQUEST['d'];
$length = $_REQUEST['n'];
$password = $_REQUEST['p'];
$symbols = $_REQUEST['s'];
$symchars = $_REQUEST['t'];
$passes = $_REQUEST['x'];

// Build the function call
$call = "/path/to/python/pswd";

if ( $caps == "false" ) {
  $call = $call . " -c";
}

$domain = preg_replace('/\s+/', '', $domain);
$domain = strtolower($domain);
$call = $call . " -d '" . $domain . "'";

$call = $call . " -n " . $length;

$call = $call . " -p '" . $password . "'";

if ( $symbols == "true" ) {
  $call = $call . " -s -t '" . $symchars . "'";
}

$call = $call . " -x " . $passes;

// Call the function
$output = shell_exec($call);

echo $output;

?>
