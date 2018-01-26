Notes about Libra
=================

conf.properties : general properties of Libra
accounts.xml : list of all handled exchanges with connection details
currencies.xml : list of all handled currencies
wallets.xml auto-generated file containing the list of currencies by exchange

How to run the program ?
1/ run Libra the first time in init mode by setting the VM arg init to true
2/ edit the wallets.xml file by setting the desired minResidualBalance as well as the fees for each currency, and the paymentId for XRP
3/ run Libra in init mode false in order to start balancing the accounts

Algorithm
- under the computed threshold (balance_check_threshold * max(init_balance, last_balance)) a rebalance is triggered
- amountToWithdraw = min((fullBalance - emptyBalance) / 2, fullBalance - minResidualBalance)