Notes about Libra
=================

libra.properties : general properties of Libra
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



Deployment
==========

inside settings.xml this server was added to <server> section in order maven to be able to connect to aws :
    <server>
        <id>ec2-node</id>                       
        <username>ec2-user</username>         
        <privateKey>/Users/Filipe/.ssh/Libra.pem</privateKey>         
    </server> 
    
command to launch maven : 
	mvn clean install -Dspring.profiles.active=prod

command to run Libra on aws :
	 nohup java -Dspring.profiles.active=prod -jar libra-0.0.2.jar &>/dev/null &
	 
	 

Releasing
=========

Branch the development branch into a release branch. Following git-flow rules, I make a release branch 1.0.

Update the POM version of the development branch. Update the version to the next release version. 
For example mvn versions:set -DnewVersion=2.0-SNAPSHOT -DgenerateBackupPoms=false. Commit and push. 
Now you can put resources developing towards the next release version.

Update the POM version of the release branch. Update the version to the standard CR version. 
For example mvn versions:set -DnewVersion=1.0.CR-SNAPSHOT -DgenerateBackupPoms=false. Commit and push.

Run tests on the release branch. Run all the tests. If one or more fail, fix them first.

Create a candidate release from the release branch.
Use the Maven version plugin to update your POM’s versions. For example mvn versions:set -DnewVersion=1.0.CR1. Commit and push.

Make a tag on git.
git tag -a 0.0.1 -m "libra-monitor v0.0.1"
git push origin 0.0.1

Use the Maven version plugin to update your POM’s versions back to the standard CR version. For example mvn versions:set -DnewVersion=1.0.CR-SNAPSHOT.

Commit and push.

Checkout the new tag.

Do a deployment build (mvn clean deploy). Since you’ve just run your tests and fixed any failing ones, this shouldn’t fail.

Put deployment on QA environment.
