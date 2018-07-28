grammar Hosh;

program
    : stmt* 
    ;

stmt
    : command ( NEWLINE | EOF )
    | command '|' stmt
    | wrapper
    ;

command
	: ID+
	;

wrapper
	: ID+ '{' command '}'
	;

ID
    : ( [a-zA-Z0-9] | '_' | ':' | '-' | '.' | '/' )+
    | '$' '{' ( [a-zA-Z0-9] | '_' | '-' )+ '}' 
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