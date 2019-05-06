grammar Hosh;

program
	: ( stmt )* 
	;

stmt
	: pipeline terminator
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

terminator
	: terminator NEWLINE
	| NEWLINE?
	;

NEWLINE
	: '\r'? '\n' -> skip
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

COMMENT
	:  '#' ~('\r' | '\n')* -> skip
	;

WS
	: [ \t]+ -> skip
	;
	