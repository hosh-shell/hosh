grammar Hosh;

program
    : stmt* 
    ;

stmt
    : command ( NEWLINE | EOF )
    | command '|' stmt
    ;

command
	: simple
	| wrapper
	;

simple
	: invocation
	;

wrapper
	: invocation '{' command '}'
	;

invocation
	: ID ( arg ) *
	;

arg
	: ID
	| VARIABLE
	;

ID
    : ( [a-zA-Z0-9] | '_' | ':' | '-' | '.' | '/' )+
    ;

VARIABLE
	: '$' '{' ( [a-zA-Z0-9] | '_' | '-' )+ '}'
	;

NEWLINE
    : '\r'? '\n'
    ;

WS
    : [ \t]+ -> skip
    ;

LINE_COMMENT
    : '#' .*? -> skip
    ;