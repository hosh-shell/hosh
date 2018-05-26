grammar Hosh;

program:
    EOF |
    stmt+ EOF
;
stmt: ID ID* NEWLINE ;
ID : [a-zA-Z0-9-]+ ;
WS : [ \t]+ -> skip ;
NEWLINE : '\n' ;
