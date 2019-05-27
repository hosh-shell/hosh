grammar Hosh;

program
	: ( stmt )* EOF
	;


stmt
	: sequence terminator?
	;

sequence
    : pipeline terminator sequence
    | pipeline
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

// by now compiler requires command (ID) to be statically defined
// later should be possible to compile line by line, performing variable expansion before compiling
invocation
	: ID ( expression )*
	;

expression
    : ( expansion )+
    | string
    ;
    
string
    : '\'' ( expansion )+ '\''
    | '"' ( expansion )+ '"' 
    ;
    
expansion
    : ID
    | VARIABLE
    | VARIABLE_OR_FALLBACK
    ;    

terminator
	:  ';'
	;

NEWLINE
	: '\r'? '\n' -> skip
	;
	
// rename to LITERAL 
ID
	: I+
	;

VARIABLE
	: '$' '{' V+ '}'
	;

VARIABLE_OR_FALLBACK
	: '$' '{' V+ '!' I+ '}'
	;

fragment I : LETTER | DIGIT | ':' | '_' | '-' | '.' | '/' | '\\' | '~' | '+';

fragment V : LETTER | '_' | '-' ;

fragment LETTER: LOWER | UPPER;

fragment LOWER: 'a'..'z';

fragment UPPER: 'A'..'Z';

fragment DIGIT: '0'..'9';

COMMENT
	:  '#' ~('\r' | '\n')* -> skip
	;

WS
	: [ \t]+ -> skip
	;
	