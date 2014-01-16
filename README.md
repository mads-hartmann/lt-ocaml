# Echo-server Example

While developing this plugin I first created a prototype
to understand how to start a server and communicate with
it from within LightTable.

Because I thought that this example might be useful for
other newbie LightTable plugin developers I stored the
example here on this branch before extending it furhter.

This code shows how to start a background process and how
to communicate with it. Additionally it shows how to display
results inline in the editor.

* `py-src/echo-server.py` contains the TCP server
* `src/lt/plugins/ocaml.cljs` contains the actual plugin code

The eaiest way to work on a plugin is to open `ocaml.cljs`
and evaluate the code straight from the editior. You have
to connect to the LightTable UI for it to work.

Happy Hacking,
Mads

