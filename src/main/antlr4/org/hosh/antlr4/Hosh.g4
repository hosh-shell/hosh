grammar Hosh;

program
    : ( stmt )* 
    ;

stmt
    : pipeline NEWLINE?
    ;

pipeline
    : command '|' stmt
    | command '|' // will be rejected by compiler
    | command
    ;

command
    : wrapped
    | simple
    ;

wrapped
    : invocation '{' stmt '}'
    | invocation '{' '}' // will be rejected by compiler
    | wrapped '}' // will be rejected by compiler 
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
    : ( [a-zA-Z0-9] | '_' | ':' | '-' | '.' | '/' | '\\' | '~' )+
    ;

STRING
	: '\'' ( ~['] )* '\''
	| '"' ( ~["] )* '"'
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