# PSWD
### A pseudo-random password generator
---
**Version**: 3.1.0

**Author**: [David Waring](http://www.davidwaring.net/)

**Email**: [dave@davidwaring.net](mailto:dave@davidwaring.net)

**Source Code**: [GitHub](https://github.com/dwaring87/pswd)

**Website**: [Web Implementation](https://pswd.davidwaring.net/)

---

PSWD is a pseudo-random password generator, meaning that it will generate a seemingly
random string of letters, numbers and symbols as the final site-specific password.  However,
when given the same set of input criteria it will generate the same password.  Passwords
generated using this method don't need to be written down or stored on your
computer or using any third-party service.  When needed, they can simply be regenerated.

The inputs required to generate a password include:

* **Username** - a nickname or email that you use to identify yourself.
* **Master Password** - a *strong* password that is used in both steps of the password
generation process.  This is the only password you will need to remember.
* **Domain** - the website or service where the final password is going to be
used.  This creates passwords that are specific for a single website or service.

Optional settings include:

* **Password Length**
* **Uppercase Letters**
* **Symbols**
* **The symbol characters to choose from**
* **The number of times the final password is hashed** - changing this is useful when
you want/need to change your password for a specific site while keeping all other
settings the same.

PSWD uses a two-step hashing process to generate the passwords as set forth in the
paper [A Convenient Method for Securely Managing Passwords (J. Alex Halderman, et.
al.; 2005)](https://jhalderm.com/pub/papers/password-www05.pdf).  This implementation
uses the **[SHA-256](http://en.wikipedia.org/wiki/SHA-2)** [cryptographic hash
function](http://en.wikipedia.org/wiki/Cryptographic_hash_function)
created by the U.S. National Security Agency.

The first step creates a "user token" by hashing the supplied username
and master password.  This step is designed to take a significant
amount of time (between 30 and 90 seconds) by repeatedly hashing
the token 10^7 times.  This is done to deter brute force attacks from
attempting to learn a master password from a stolen site password.  As
a compromise for convenience, this user token can be locally cached
so it does not have to be computed each time a password is generated.

The second step involves hashing the cached user token, the master
password and the domain.  The hash function is repeated many fewer
times so this step is almost instantaneous.  The end result is
a site-specific hash using the digits 0-9 and letters a-f.

The final step is processing any of the optional settings (password
length, uppercase letters and symbols).  When given the same input
settings, these options will be applied in the same manner every time.
This ensures that the same exact password will be generated each
time.

<img src="https://pswd.davidwaring.net/images/general.png" width="450px">




## Implementations

The following implementations are currently available:

* **Python** - This provides a command line interface that should
work across different systems.

* **Android** - This is an Eclipse project that includes a fully-functional
Android app implementing the PSWD v3 algorithm in Java.  The app will
cache User Tokens as encrypted strings in the app's SharedPreferences.

* **Java** - These are java functions that are used in the Android
application.

* **Web** - This is a web front-end that caches the user token
as an encrypted cookie and passes all arguments to the Python
script which is run on the server.  This uses Twitter Bootstrap v3
and php scripts for encryption and the system call to pswd.  This
is  a copy of [https://pswd.davidwaring.net/](https://pswd.davidwaring.net/)


## Usage
The following is the usage of the Python command line script.
```
PSWD: A pseudo-random password generator
Version: 3.1.0
Author: David Waring
Information: https://pswd.davidwaring.net/info.html

USAGE:
  pswd --user [user name] --password [master password] --domain [domain]
REQUIRED ARGUMENTS:
  -u, --user [user name]: a user name to identify yourself
  -p, --password [master password]: your master password
  -d, --domain [domain]: the domain the password will be used for
OPTIONAL ARGUMENTS:
  -n [int]: the length of the final password (Default: 24)
      minimum: 4    maximum: 64
  -c: toggle the use of uppercase letters (Default: True)
  -s: toggle the use of symbols (Default: True)
  -a [symbols]: define possible symbols to include (Default: !@$*-_.?)
  -x [int]: the number of times the user token is hashed (Default: 10000000)
      the user token is generated once when the script is first run and cached
      for subsequent uses.  This is designed to take some time to generate.
  -y [int]: the number of times the generated password is hashed (Default: 250)
      this number can be changed to generate a new password for the same
      domain while keeping all other settings the same.
  -q: don't print the password to the console (just copy to clipboard)
  --cache: location of the user token cache file
  -h, --help: display this usage information.

If you want to manage the user token yourself (instead of using the cache file):
  To retrieve or generate a user token:
    pswd --gettoken --user [user name] --password [master password]
  When generating a password, add the token in addition to other arguments:
    -t, --token [user token]: provide your own user token
```


## Algorithm Details
The following image details the steps of the PSWD algorithm with the right half
showing the generation of an example password for each step.  The image is
also available as a PDF from here: https://pswd.davidwaring.net/images/algo.pdf

The dark green fields indicate user-entered variables and options.  The light
green fields indicate calculated variables.  The '+' sign shows when
two or more variables were concatenated together to form a single variable.

<img src="https://pswd.davidwaring.net/images/algo.png" />
