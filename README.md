TurkServer

multi-user experiment framework for Amazon Mechanical Turk

TurkServer builds a framework for real-time communication and group
tasks on top of Amazon Mechanical Turk (MTurk). Features include
real-time communication between clients and a server, user grouping,
lobbying, and coordination abilities, and capabilities for tracking
users over time. The framework is targeted toward experimenters
interested in social or interface experiments on MTurk but can also be
useful in many other circumstances.

Programming with TurkServer involves writing server logic in Java,
which hooks up to the server API, and writing a (usually graphical)
client interface using either a Java applet or
Javascript/HTML. TurkServer takes care of network communication,
posting hits on MTurk, and many other aspects for you. However, you do
need to have some programming knowledge; it isn't a completely
point-and-click framework like zTree.

TurkServer is currently in the development stage. You can check out
the core code via SVN from this Google Code project. We have developed
several experiment harnesses using TurkServer, such as distributed
graph coloring and market maker/trading interfaces, but these are not
part of the core code. However, we expect to release them as examples
once the projects are finished.
