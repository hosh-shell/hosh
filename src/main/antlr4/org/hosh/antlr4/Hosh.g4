grammar Hosh;

program
	: ( stmt )* EOF
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

// commands must be statically resolved before execution 
// this is way hosh requires ID and not VARIABLE here
invocation
	: ID ( arg )*
	;

arg
	: ID
	| STRING
	| VARIABLE
	;

terminator
	: terminator ';'
	| ';'?
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
	