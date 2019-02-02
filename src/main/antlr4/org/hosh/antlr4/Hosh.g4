grammar Hosh;

program
    : ( stmt )* 
    ;

stmt
    : pipeline NEWLINE?
    | wrapped NEWLINE?
    | single NEWLINE?
    ;

pipeline
    : invocation '|' stmt
    ;

wrapped
    : invocation '{' stmt '}'
    ;

single
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