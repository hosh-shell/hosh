grammar Hosh;

program
    : stmt* 
    ;

stmt
    : command end_of_statement
    | command '|' stmt
    ;

command
	: ID+
	;

end_of_statement
	: NEWLINE
	| EOF
	;

ID
    : ( [a-zA-Z0-9] | '_' | ':' | '-' | '.' | '/' )+
    | '$' '{' ( [a-zA-Z0-9] | '_' | '-' )+ '}' // this could VARNAME, to remove logic from compiler 
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