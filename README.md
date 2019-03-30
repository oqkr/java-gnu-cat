# ~~jat~~ cat
A toy program for learning Java by reimplementing GNU core utils.

## How do I run it?

Good question. I think you can just open the project in IntelliJ and everything
should Just Work, but my IntelliJ and Maven skills are still weak.

## Requirements

At least Java 10, but maybe Java 11 — I'm not sure: I've only been using Java
for a week. 😳

## Bugs

Obviously no one should use this ever.

But these are the known issues so far:

* `-v`, `--show-nonprinting` option is not implemented because I haven't
  invested the time to understand what it does. Using GNU cat,
  `printf 🦄 | cat -v` outputs `M-pM-^_M-&M-^D`. Why? ヽ(´ー｀)┌
* Passing the same option multiple times (e.g., `cat -n -n -n`) attempts to
  apply the same option multiple times on the already transformed input, which
  is almost certainly never what you want.
* Not quite a bug but a difference: In GNU cat, if a file doesn't exist or
  can't be read, execution continues on to the next file after printing an
  error. This version of cat, however, tries to read all the files up front
  and crashes without doing anything at all if any one of the files can't be
  read.
 