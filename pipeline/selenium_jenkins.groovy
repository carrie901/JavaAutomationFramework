import hudson.model.*;

pipeline{

    agent any
    parameters {
        string(name: 'BROWSER_TYPE', defaultValue: 'chrome', description: 'Type a browser type, should be chrome/firefox')
        string(name: 'TEST_SERVER_URL', defaultValue: '', description: 'Type the test server url')
        string(name: 'NODE', defaultValue: 'win-anthony-demo', description: 'Please choose a windows node to execute this job.')
    }
    
	stages{
	    stage("Initialization"){
	        steps{
	            script{
	                browser_type = BROWSER_TYPE?.trim()
	                test_url = TEST_SERVER_URL?.trim()
	                win_node = NODE?.trim()
	            }
	        }
	    }

	    stage("Git Checkout"){
	        steps{
	            script{
	                node(win_node) {
	                     checkout([$class: 'GitSCM', branches: [[name: '*/master']],
						    userRemoteConfigs: [[credentialsId: '6f4fa66c-eb02-46dc-a4b3-3a232be5ef6e', 
							url: 'https://github.com/QAAutomationLearn/JavaAutomationFramework.git']]])
	                }
	            }
	        }
	    }
	    
        stage("Set key value"){
	        steps{
	            script{
	                node(win_node){
	                    selenium_test = load env.WORKSPACE + "\\pipeline\\selenium.groovy"
	                    config_file = env.WORKSPACE + "\\Configs\\config.properties"
	                    try{
	                        selenium_test.setKeyValue("browser", browser_type, config_file)
	                        file_content = readFile config_file
                            println file_content
	                    }catch (Exception e) {
	                        error("Error met:" + e)
	                    }
	                }
	            }
	        }
	    }
	    
	    stage("Run Selenium Test"){
	        steps{
	            script{
	                node(win_node){
	                    run_bat = env.WORKSPACE + "\\run.bat"
	                    bat (run_bat)
	                }
	            }
	        }
	    }
	    stage("Publish Selenium HTML Report"){
	        steps{
	            script{
	                node(win_node){
	                   html_file_name = selenium_test.get_html_report_filename("test-output")
	                   publishHTML (target: [
                        	allowMissing: false,
                        	alwaysLinkToLastBuild: false,
                        	keepAll: true,
                        	reportDir: 'test-output',
                        	reportFiles: html_file_name,
                        	reportName: "Selenium Test Report"
                    	])
	                }
	            }
	        }
	    }
	}

    post{
        always{
            script{
                node(win_node){
                    //delete report file
                    println "Start to delete old html report file."
                    bat("del /s /q C:\\JenkinsNode\\workspace\\selenium-pipeline-demo\\test-output\\*.html")
                    //archive the log files on jenkins ui
                    archive 'log/*.*'
                    println "Start to delete old log files."
                    bat("del /s /q C:\\JenkinsNode\\workspace\\selenium-pipeline-demo\\log\\*.*")
                }
            }
        }
    }

}
