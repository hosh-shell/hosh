grammar Hosh;

program
    : ( stmt )* 
    ;

stmt
    : pipeline NEWLINE?
    | command NEWLINE?
    ;

pipeline
    : command '|' stmt
    | command '|' // will be rejected by the compiler with a nice error message
    ;

command
    : wrapped
    | simple
    ;

wrapped
    : invocation '{' stmt '}'
    | wrapped '}' // will be rejected by the compiler with a nice error message
    ;

simple
    : invocation
    ;

invocation
	: ID ( arg )*
	;

arg
	: ID
	| STRING
	| VARIABLE
	;

ID
    : ( [a-zA-Z0-9] | '_' | ':' | '-' | '.' | '/' | '\\' )+
    ;

STRING
	: '\'' ( ~['\\] )* '\''
	| '"' ( ~["\\] )* '"'
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