del /F /Q W:\Libra\bin\*.*
del /F /Q W:\Libra\conf\*.*
del /F /Q W:\Libra\lib\*.*
del /F /Q W:\Libra\logs\*.*
del /F /Q W:\Libra\*.*
rd W:\Libra\logs
rd W:\Libra\lib
rd W:\Libra\conf
rd W:\Libra\bin
md W:\Libra
md W:\Libra\logs
md W:\Libra\lib
md W:\Libra\conf
md W:\Libra\bin
md W:\Libra\database
xcopy "./target" "W:\Libra" /Y /E
xcopy "./Win32Service/bin" "W:\Libra\bin" /Y /E
xcopy "./Win32Service/conf" "W:\Libra\conf" /Y /E
xcopy "./Win32Service/lib" "W:\Libra\lib" /Y /E
xcopy "./Win32Service/logs" "W:\Libra\logs" /Y /E
copy /Y src\main\resources\conf.properties W:\Libra\conf\conf.properties
copy /Y src\main\resources\data\accounts.xml W:\Libra\conf\accounts.xml
copy /Y src\main\resources\data\currencies.xml W:\Libra\conf\currencies.xml
copy /Y src\main\resources\data\generated\wallets.xml W:\Libra\conf\wallets.xml
copy /Y database\libra.db W:\Libra\database\libra.db