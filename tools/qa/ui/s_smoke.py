from cap import *
cond("tr", "DARK", "MEDIUM", 1.0, "small")
load("us_erken")
print(shot("us_erken"))
for t in texts():
    print(t)
