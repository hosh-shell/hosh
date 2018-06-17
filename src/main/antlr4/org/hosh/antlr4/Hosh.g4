grammar Hosh;

program
    : stmt* 
    ;

stmt
    : ID+ eos
    | ID+  '|' stmt
    ;


eos
	: NEWLINE
	| EOF
	;

ID
    : ([a-zA-Z0-9] | '-' | '.' | '/') +
    ;

NEWLINE
    : '\r'? '\n'
    ;

WS
    : [ \t]+ -> skip
    ;

LINE_COMMENT
    : '#' .*? '\r'? '\n' -> skip
    ;