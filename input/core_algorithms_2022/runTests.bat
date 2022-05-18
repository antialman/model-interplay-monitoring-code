@echo off

::
:: Test 01 Model size without data
::

echo START: 01 Model size without data

for /l %%x in (0, 1, 6) do (
	echo 01 Model size without data - test no. %%x >> testResults.txt
	echo AutTime	AutStates	EventAvg>> testResults.txt
	for /l %%y in (1, 1, 5) do (
		java -Xmx28672m -jar ..\..\target\model-interplay.jar -c .\01_modelSize_noData\test0%%x_costModel.txt -l .\logGen\gen_eventlog.xes -statsFile testResults.txt --cmd
		timeout 2 > NUL
	)
	echo.>> testResults.txt
)

echo.>> testResults.txt
echo.>> testResults.txt

echo DONE: 01 Model size without data


::
:: 02 Model size with data
::

echo START: 02 Model size with data

for /l %%x in (0, 1, 6) do (
	echo 02 Model size with data - test no. %%x >> testResults.txt
	echo AutTime	AutStates	EventAvg>> testResults.txt
	for /l %%y in (1, 1, 5) do (
		java -Xmx28672m -jar ..\..\target\model-interplay.jar -c .\02_modelSize_wData\test0%%x_costModel.txt -l .\logGen\gen_eventlog.xes -statsFile testResults.txt --cmd
		timeout 2 > NUL
	)
	echo.>> testResults.txt
)

echo.>> testResults.txt
echo.>> testResults.txt

echo DONE: 02 Model size with data


::
:: 03 Guard placement in control flow
::

echo START: 03 Guard placement in control flow

for /l %%x in (0, 1, 7) do (
	echo 03 Guard placement in control flow - test no. %%x >> testResults.txt
	echo AutTime	AutStates	EventAvg>> testResults.txt
	for /l %%y in (1, 1, 5) do (
		java -Xmx28672m -jar ..\..\target\model-interplay.jar -c .\03_conditionPlacement\test0%%x_costModel.txt -l .\logGen\gen_eventlog.xes -statsFile testResults.txt --cmd
		timeout 2 > NUL
	)
	echo.>> testResults.txt
)

echo.>> testResults.txt
echo.>> testResults.txt

echo DONE: 03 Guard placement in control flow


::
:: 04 Number of guards (beginning -> end)
::

echo START: 04 Number of guards from beginning

for /l %%x in (0, 1, 7) do (
	echo 04 Number of guards from beginning - test no. %%x >> testResults.txt
	echo AutTime	AutStates	EventAvg>> testResults.txt
	for /l %%y in (1, 1, 5) do (
		java -Xmx28672m -jar ..\..\target\model-interplay.jar -c .\04_conditionCount_begin\test0%%x_costModel.txt -l .\logGen\gen_eventlog.xes -statsFile testResults.txt --cmd
		timeout 2 > NUL
	)
	echo.>> testResults.txt
)

echo.>> testResults.txt
echo.>> testResults.txt

echo DONE: 04 Number of guards from beginning


::
:: 06 Syncronisation activity placement in control flow
::

echo START: 06 Syncronisation activity placement in control flow

for /l %%x in (0, 1, 6) do (
	echo 06 Syncronisation activity placement in control flow - test no. %%x >> testResults.txt
	echo AutTime	AutStates	EventAvg>> testResults.txt
	for /l %%y in (1, 1, 5) do (
		java -Xmx28672m -jar ..\..\target\model-interplay.jar -c .\06_sync_placement\test0%%x_costModel.txt -l .\logGen\gen_eventlog.xes -statsFile testResults.txt --cmd
		timeout 2 > NUL
	)
	echo.>> testResults.txt
)

echo.>> testResults.txt
echo.>> testResults.txt

echo DONE: 06 Syncronisation activity placement in control flow


::
:: 07 Number of syncronisation activities (beginning -> end)
::

echo START: 07 Number of syncronisation activities from beginning

for /l %%x in (0, 1, 6) do (
	echo 07 Number of syncronisation activities from beginning - test no. %%x >> testResults.txt
	echo AutTime	AutStates	EventAvg>> testResults.txt
	for /l %%y in (1, 1, 5) do (
		java -Xmx28672m -jar ..\..\target\model-interplay.jar -c .\07_syncCount_begin\test0%%x_costModel.txt -l .\logGen\gen_eventlog.xes -statsFile testResults.txt --cmd
		timeout 2 > NUL
	)
	echo.>> testResults.txt
)

echo.>> testResults.txt
echo.>> testResults.txt

echo DONE: 07 Number of syncronisation activities from beginning


::
:: 09 Number of input models without data
::

echo START: 09 Number of input models without data

for /l %%x in (0, 1, 5) do (
	echo 09 Number of input models without data - test no. %%x >> testResults.txt
	echo AutTime	AutStates	EventAvg>> testResults.txt
	for /l %%y in (1, 1, 5) do (
		java -Xmx28672m -jar ..\..\target\model-interplay.jar -c .\09_modelCount_noData\test0%%x_costModel.txt -l .\logGen\gen_eventlog.xes -statsFile testResults.txt --cmd
		timeout 2 > NUL
	)
	echo.>> testResults.txt
)

echo.>> testResults.txt
echo.>> testResults.txt

echo DONE: 09 Number of input models without data


::
:: 10 Number of input models with data
::

echo START: 10 Number of input models with data

for /l %%x in (0, 1, 5) do (
	echo 10 Number of input models with data - test no. %%x >> testResults.txt
	echo AutTime	AutStates	EventAvg>> testResults.txt
	for /l %%y in (1, 1, 5) do (
		java -Xmx28672m -jar ..\..\target\model-interplay.jar -c .\10_modelCount_wData\test0%%x_costModel.txt -l .\logGen\gen_eventlog.xes -statsFile testResults.txt --cmd
		timeout 2 > NUL
	)
	echo.>> testResults.txt
)

echo.>> testResults.txt
echo.>> testResults.txt

echo DONE: 10 Number of input models with data


