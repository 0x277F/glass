Glass
=====
*"The Worst Bouncer"™*

Glass is a simple proxy server for IRC. I wrote it to try and supplement 
Quassel's lack of extensibility as a client. In the interest of not duplicating
Quassel's functionality, Glass doesn't provide bouncer features such as
persistent connections or backlog storage. I sincerely doubt anyone else will
find this useful, but have fun.

Usage
-----
The proxy can handle any number of IRC connections, each of which is specified
by one of two methods. Glass provides the `SYN <address> <port> [-s]`, with `-s`
denoting an SSL connection (must be the 3rd argument). Once connected to the proxy,
the client may send this command to establish an upstream connection to an IRC server.
Because the downstream connections are established before the upstream connections, 
some clients may have issues automatically establishing a connection. To work around
this, the proxy will intercept and store the client's `NICK` and `USER` commands, which
will be sent to the server once the upstream connection is established. To automatically
establish this connection, the `USER` line's real name field is parsed for a string after
the delimiter `##`. This string is then treated as a command from the client, meaning the
`SYN` line may be placed here. 

For example - to connect to Freenode, you may set your client's "Real Name" field to 
`Hex##SYN chat.freenode.net 6697 -s`. Upon getting the client's `USER` command, the proxy
will establish an upstream connection to `chat.freenode.net` on port `6697` with SSL enabled,
passing `Hex` as the real name to the server. It will also pass a `NICK` command, if your client
has sent one already.

To-Do
-----
* Support for server passwords. Because server passwords are sent before `NICK` and `USER`, they need
to be cached by the proxy.
* Commands to interact with backlogs and Quassel's sqlite backend.
* Cleaner handling for proxy restarts and shutdowns.
* Maybe document the code or something.

Special thanks to [kashike](https://github.com/kashike) for compensating for my incompetence.