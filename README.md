# OCaml Plugin for LightTable

Currently there's not much to see here. For now it really
doesn't do anything - I'm using it to learn how the various
components of LightTable work together.

Whenever I figure out how something works I try to create
a minimal example and put it on a branch. Here's what I have
so far

* [Connecting and communicating with a TCP server](https://github.com/mads379/lt-ocaml/tree/ehco-server-example)

## Installing it

For now I assume that you're on OS X given that it's the
only platform I've tested this on.

To install it clone the project to
`~/Library/Application Support/LightTable/plugins/`

To load the plugin:

* Run the command `Plugins: Refresh plugin list`
* Run the command `App: Reload behaviors`

## Try it out

Open a OCaml file (uses extension `.ml`) write
something and hit `cmd-enter`. You might have to run
`App: Reload behaviors` first.

## Hacking on it

The eaiest way to work on a plugin is to open ocaml.cljs and evaluate the code straight from the editior. You have to connect to the LightTable UI for it to work.