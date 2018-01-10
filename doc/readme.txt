Notes about Libra
=================

conf.properties : general properties of Libra
accounts.xml : list of all handled exchanges with connection details
currencies.xml : list of all handled currencies
balances.xml auto-generated file containing the list of currencies by exchange

How to run the program ?
1/ run Libra in init mode by setting the VM arg init to true
2/ edit the balances.xml file by setting the desired minResidualBalance for each currency
3/ run Libra in init mode false in order to start balancing the accounts