TurkServer 

multi-user experiment framework for Amazon Mechanical Turk

**October 2013 update: We are building the [next generation of
 TurkServer](https://github.com/HarvardEconCS/turkserver-meteor). If
 you are just getting started with an experimental project, you will
 want to use the [Meteor](http://www.meteor.com/) version of
 TurkServer instead. It accomplishes everything that this version does
 with even less code and allows you to focus on designing and running
 your experiment, not technical minutiae.**
	    
TurkServer builds a framework for real-time communication and group
tasks on top of Amazon Mechanical Turk (MTurk). Features include
real-time communication between clients and a server, user grouping,
lobbying, and coordination abilities, and capabilities for tracking
users over time. The framework is targeted toward experimenters
interested in human computation, social science, user interface
experiments on MTurk but can also be useful in many other
circumstances. 

Turkserver isn't a point-and-click framework like zTree; you need to
have some programming experience in Java, and knowledge of user
interface design using Java's Swing or client-side
Javascript. However, TurkServer simplifies many other things that you
would otherwise have to do from scratch, such as network
communication, posting hits on MTurk, paying workers, and collecting
data. [Please read the wiki](https://github.com/HarvardEconCS/TurkServer/wiki) to learn more about how
to use TurkServer. As with any other open-source project, we welcome
useful collaboration: bug reports, help with the documentation, and
good pull requests.

There are no versioned releases yet, but you can check out the core
code via Git. We are currently updating documentation for TurkServer,
and we plan to release some example experiments and interfaces in the
near future once the corresponding projects are finished.
