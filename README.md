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
