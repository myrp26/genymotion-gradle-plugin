package main.groovy.com.genymotion

import org.codehaus.groovy.runtime.NullObject

/**
 * Created by eyal on 10/09/14.
 */
class GenymotionTool {

    public static GenymotionConfig GENYMOTION_CONFIG = null

    private static final String GENYTOOL =  "gmtool"

    //root actions
    private static final String SETLICENSE =  "setlicense"
    private static final String CONFIG =  "config"
    private static final String LOGZIP =  "logzip"
    //admin actions
    private static final String ADMIN =     "admin"
    private static final String LIST =         "list" //"VBoxManage list "
    private static final String TEMPLATES =    "templates"
    private static final String CREATE =       "create"
    private static final String UPDTAE =       "update"
    private static final String DELETE =       "delete"
    private static final String CLONE =        "clone"
    private static final String DETAILS =      "details"
    private static final String START =        "start"
    private static final String RESTART =      "restart"
    private static final String STOP =         "stop"
    private static final String STOPALL =      "stopall"
    private static final String RESET =        "factoryreset"
    private static final String STARTAUTO =    ""//TODO
    //device actions
    private static final String DEVICE =    "device"
    private static final String PUSH =         "push"
    private static final String PULL =         "pull"
    private static final String INSTALL =      "install"
    private static final String FLASH =        "flash"
    private static final String LOGCAT =       "logcat"
    private static final String ADBDISCONNECT = "adbdisconnect"
    private static final String ADBCONNECT =   "adbconnect"

    //code returned by gmtool or command line
    public static final int RETURN_DEVICE_NOT_FOUND       = -1
    public static final int RETURN_NO_ERROR               = 0
    public static final int RETURN_GENERIC_ERROR          = 1
    public static final int RETURN_NO_SUCH_ACTION         = 2
    public static final int RETURN_CANT_LOGIN             = 3
    public static final int RETURN_CANT_REGISTER_LICENSE  = 4
    public static final int RETURN_CANT_ACTIVATE_LICENSE  = 5
    public static final int RETURN_NO_ACTIVATED_LICENSE   = 6
    public static final int RETURN_INVALID_LICENSE        = 7
    public static final int RETURN_PENDING_ACTION         = 8
    public static final int RETURN_ARGS_ERROR             = 9
    public static final int RETURN_VM_NOT_STOPPED         = 10
    public static final int RETURN_COMMAND_NOT_FOUND_UNIX = 127


    static def usage(){
        return cmd([GENYTOOL, "-h"]){line, count ->
        }
    }

    /*
    CONFIG
     */

    static def setLicense(String license, String login="", String password=""){
        return cmd([GENYTOOL, SETLICENSE, license, "-l="+login, "-p="+password]){line, count ->
        }
    }

    static def resetConfig(){
        return cmd([GENYTOOL, CONFIG, "--reset"]){line, count ->
        }
    }

    static def clearCache(){
        return cmd([GENYTOOL, CONFIG, "--clearcache"]){line, count ->
        }
    }

    static def logzip(String path="", String vdName=""){

        def command = [GENYTOOL, LOGZIP]

        if(vdName?.trim())
            command.push(["-n ", vdName])

        if(path?.trim())
            command.push(path)

        return cmd([GENYTOOL, LOGZIP, "-n=", ]){line, count ->
        }
    }

    static def config(){
        //TODO implement when gmtool is ready

    }



    /*
    ADMIN
     */

    static def getAllDevices(boolean verbose=false, boolean fill=true, boolean nameOnly=false){

        def devices = []

        cmd([GENYTOOL, ADMIN, LIST], verbose){line, count ->
            def device = parseList(count, line, nameOnly)
            if(device)
                devices.add(device)
        }

        if(fill && !nameOnly){
            devices.each(){
                it.fillFromDetails()
            }
        }

        devices
    }

    static def getRunningDevices(boolean verbose=false, boolean fill=true, boolean nameOnly=false){

        def devices = []

        cmd([GENYTOOL, ADMIN, LIST, "--running"], verbose){line, count ->
            def device = parseList(count, line, nameOnly)
            if(device)
                devices.add(device)
        }

        if(fill && !nameOnly){
            devices.each(){
                it.fillFromDetails()
            }
        }

        devices
    }

    static def getStoppedDevices(boolean verbose=false, boolean fill=true, boolean nameOnly=false){

        def devices = []

        cmd([GENYTOOL, ADMIN, LIST, "--off"], verbose){line, count ->
            def device = parseList(count, line, nameOnly)
            if(device)
                devices.add(device)
        }

        if(fill && !nameOnly){
            devices.each(){
                it.fillFromDetails()
            }
        }

        devices
    }

    private static def parseList(int count, String line, boolean nameOnly) {

        def device

        //we skip the first lines
        if (count < 2)
            return

        String[] infos = line.split('\\|')

        String name = infos[3].trim()
        if (nameOnly) {
            device = name
        } else {
            device = new GenymotionVirtualDevice(name)
            device.ip = infos[2].trim()
            device.state = infos[1].trim()
        }
        device
    }


    static def isDeviceCreated(String name){

        if(!name?.trim())
            return false

        //we check if the VD name already exists
        boolean alreadyExists = false

        def devices = GenymotionTool.getAllDevices(false, false)

        devices.each(){
            if(it.name.equals(name))
                alreadyExists = true
        }
        alreadyExists
    }

    static def getTemplatesNames(boolean verbose=false) {

        def templates = []

        def template = null

        cmd([GENYTOOL, ADMIN, TEMPLATES], verbose) { line, count ->

            //if empty line and template filled
            if (!line && template){
                templates.add(template)
                template = null
            }

            String[] info = line.split("\\:")
            switch (info[0].trim()){
                case "Name":
                    if(!template)
                        template = info[1].trim()
                    break
            }
        }
        if(template)
            templates.add(template)

        return templates
    }

    static def getTemplates(boolean verbose=false){

        def templates = []

        def template = new GenymotionTemplate()

        cmd([GENYTOOL, ADMIN, TEMPLATES, "--full"], verbose) { line, count ->

            //if empty line and template filled
            if (!line && template.name){
                templates.add(template)
                template = new GenymotionTemplate()
            }

            String[] info = line.split("\\:")
            switch (info[0].trim()){
                case "Name":
                    if(!template.name)
                        template.name = info[1].trim()
                    break
                case "UUID":
                    template.uuid = info[1].trim()
                    break
                case "Description":
                    template.description = info[1].trim()
                    break
                case "Android Version":
                    template.androidVersion = info[1].trim()
                    break
                case "Genymotion Version":
                    template.genymotionVersion = info[1].trim()
                    break
                case "Screen Width":
                    template.width = info[1].trim().toInteger()
                    break
                case "Screen Height":
                    template.height = info[1].trim().toInteger()
                    break
                case "Screen Density":
                    template.density = info[1].trim()
                    break
                case "Screen DPI":
                    template.dpi = info[1].trim().toInteger()
                    break
                case "Nb CPU":
                    template.nbCpu = info[1].trim().toInteger()
                    break
                case "RAM":
                    template.ram = info[1].trim().toInteger()
                    break
                case "Internal Storage":
                    template.internalStorage = info[1].trim().toInteger()
                    break
                case "Telephony":
                    template.telephony = info[1].trim().toBoolean()
                    break
                case "Nav Bar Visible":
                    template.navbarVisible = info[1].trim().toBoolean()
                    break
                case "Virtual Keyboard":
                    template.virtualKeyboard = info[1].trim().toBoolean()
                    break
            }

        }
        if(template.name)
            templates.add(template)

        return templates
    }

    static boolean isTemplateExists(String template, boolean verbose=false) {

        if(!template?.trim())
            return false

        def templates = getTemplatesNames(verbose)
        templates.contains(template)
    }

    static def createDevice(GenymotionVirtualDevice device){
        return createDevice(device.template, device.name, device.dpi, device.width, device.height, device.virtualKeyboard, device.navbarVisible, device.nbCpu, device.ram)
    }

    static def createDevice(def template, def deviceName, def dpi="", def width="", def height="", def virtualKeyboard="", def navbarVisible="", def nbcpu="", def ram=""){

        return noNull(){
            cmd([GENYTOOL, ADMIN, CREATE, template, deviceName,
                 '--dpi='+dpi, '--width='+width, '--height='+height, '--virtualkeyboard='+virtualKeyboard, '--navbar='+navbarVisible, '--nbcpu='+nbcpu, "--ram="+ram]){line, count ->
                //TODO check the request's result
                //TODO add the apiLevel into the created device
                //if ok: return the device created
                def device = new GenymotionVirtualDevice(deviceName, null, dpi, width, height, virtualKeyboard, navbarVisible, nbcpu, ram)
            }
        }
    }

    static def updateDevice(GenymotionVirtualDevice device){
        return updateDevice(device.name, device.dpi, device.width, device.height, device.virtualKeyboard, device.navbarVisible, device.nbCpu, device.ram)
    }

    static def updateDevice(def deviceName, def dpi="", def width="", def height="", def virtualKeyboard="", def navbarVisible="", def nbcpu="", def ram=""){

        return noNull(){
            return cmd([GENYTOOL, ADMIN, UPDTAE, deviceName,
                 '--dpi='+dpi, '--width='+width, '--height='+height, '--virtualkeyboard='+virtualKeyboard, '--navbar='+navbarVisible, '--nbcpu='+nbcpu, "--ram="+ram]){line, count ->
            }
        }
    }

    static def deleteDevice(GenymotionVirtualDevice device){
        return deleteDevice(device.name)
    }

    static def deleteDevice(def deviceName){
        return cmd([GENYTOOL, ADMIN, DELETE, deviceName]){line, count ->
            //TODO check the request's result
        }
    }

    static def cloneDevice(GenymotionVirtualDevice device, def name){
        return cloneDevice(device.name, name)
    }

    static def cloneDevice(def deviceName, def newName){
        return cmd([GENYTOOL, ADMIN, CLONE, deviceName, newName]){line, count ->
            //TODO check the request's result
        }
    }

    static def getDevice(String name, boolean verbose=false){

        if(name == null)
            return null

        def device = new GenymotionVirtualDevice(name)
        device = getDevice(device, verbose)
    }

    static def getDevice(def device, boolean verbose=false){

        if(device == null)
            return null

        //we get the device details
        cmd([GENYTOOL, ADMIN, DETAILS, device.name], verbose){line, count ->

            //we skip the first line
            if(count < 1)
                return

            String[] info = line.split("\\:")
            switch (info[0].trim()){
                case "Name":
                    device.name = info[1].trim()
                    break
                case "Android Version":
                    device.androidVersion = info[1].trim()
                    break
                case "Genymotion Version":
                    device.genymotionVersion = info[1].trim()
                    break
                case "Screen Width":
                    device.width = info[1].trim().toInteger()
                    break
                case "Screen Height":
                    device.height = info[1].trim().toInteger()
                    break
                case "Screen Density":
                    device.density = info[1].trim()
                    break
                case "Screen DPI":
                    device.dpi = info[1].trim().toInteger()
                    break
                case "Nb CPU":
                    device.nbCpu = info[1].trim().toInteger()
                    break
                case "RAM":
                    device.ram = info[1].trim().toInteger()
                    break
                case "Telephony":
                    device.telephony = info[1].trim().toBoolean()
                    break
                case "Nav Bar Visible":
                    device.navbarVisible = info[1].trim().toBoolean()
                    break
                case "Virtual Keyboard":
                    device.virtualKeyboard = info[1].trim().toBoolean()
                    break
                case "UUID":
                    device.uuid = info[1].trim()
                    break
                case "Path":
                    device.path = info[1].trim()
                    break
                case "State":
                    device.state = info[1].trim()
                    break
                case "IP":
                    device.ip = info[1].trim()
                    break
            }
        }
        device
    }

    static def startDevice(GenymotionVirtualDevice device){
        return startDevice(device.name)
    }

    static def startDevice(def deviceName){
        return cmd([GENYTOOL, ADMIN, START, deviceName]) {line, count ->
        }
    }

    static def restartDevice(GenymotionVirtualDevice device){
        return restartDevice(device.name)
    }

    static def restartDevice(def deviceName){
        return cmd([GENYTOOL, ADMIN, RESTART, deviceName]){line, count ->
            //TODO check the request's result
        }
    }

    static def stopDevice(GenymotionVirtualDevice device){
        return stopDevice(device.name)
    }

    static def stopDevice(def deviceName){
        return cmd([GENYTOOL, ADMIN, STOP, deviceName]){line, count ->
            //TODO check the request's result
        }
    }

    static def stopAllDevices(){
        return cmd([GENYTOOL, ADMIN, STOPALL]){line, count ->
            //TODO check the request's result
        }
    }

    static def resetDevice(GenymotionVirtualDevice device){
        return resetDevice(device.name)
    }

    static def resetDevice(def deviceName){
        return cmd([GENYTOOL, ADMIN, START, RESET, deviceName]){line, count ->
            //TODO check the request's result
        }
    }

    static def startAutoDevice(def template, def apiLevel){
        def device = createDevice(template, apiLevel, "")
        return startDevice(device)
        //TODO check if we need to provide a name
    }


    /*
    Device
     */

    static def pushToDevice(GenymotionVirtualDevice device, def files){
        pushToDevice(device.name, files)
    }

    static def pushToDevice(def deviceName, def files){

        if(!files)
            return false

        files.each(){

            def command = [GENYTOOL, DEVICE, deviceName, PUSH]
            if(files instanceof Map)
                command.push([it.key, it.value])
            else
                command.push(it)

            cmd(command){line, count ->
            }
        }
        //TODO Check the result when exit codes will be implemented on gmtool
    }

    static def pullFromDevice(GenymotionVirtualDevice device, def files){
        pullFromDevice(device.name, files)
    }

    static def pullFromDevice(def deviceName, def files){

        if(!files)
            return false

        files.each(){

            def command = [GENYTOOL, DEVICE, deviceName, PULL]
            if(files instanceof Map)
                command.push([it.key, it.value])
            else
                command.push(it)

            cmd(command){line, count ->
            }
        }
        //TODO Check the result when exit codes will be implemented on gmtool
    }

    static def installToDevice(GenymotionVirtualDevice device, def apks){
        installToDevice(device.name, apks)
    }

    static def installToDevice(def deviceName, def apks){

        if(!apks)
            return false

        if(apks instanceof String){
            cmd([GENYTOOL, DEVICE, deviceName, INSTALL, apks]){line, count ->
            }

        } else if(apks instanceof String[]){
            apks.each(){
                cmd([GENYTOOL, DEVICE, deviceName, INSTALL, it]){line, count ->
                }
            }
        }
        //TODO Check the result when exit codes will be implemented on gmtool
    }

    static def flashDevice(GenymotionVirtualDevice device, def zips){
        flashDevice(device.name, zips)
    }

    static def flashDevice(def deviceName, def zips){

        if(!zips)
            return false

        if(zips instanceof String){
            cmd([GENYTOOL, DEVICE, deviceName, FLASH, zips]){line, count ->
            }

        } else if(zips instanceof String[]){
            zips.each(){
                cmd([GENYTOOL, DEVICE, deviceName, FLASH, it]){line, count ->
                }
            }
        }
        //TODO Check the result when exit codes will be implemented on gmtool
    }

    static def adbDisconnectDevice(GenymotionVirtualDevice device){
        adbDisconnectDevice(device.name)
    }

    static def adbDisconnectDevice(def deviceName){
        cmd([GENYTOOL, DEVICE, deviceName, ADBDISCONNECT]){line, count ->
        }
        //TODO Check the request's feedback
    }

    static def adbConnectDevice(GenymotionVirtualDevice device){
        adbConnectDevice(device.name)
    }

    static def adbConnectDevice(def deviceName){
        cmd([GENYTOOL, DEVICE, deviceName, ADBCONNECT]){line, count ->
        }
        //TODO Check the request's feedback
    }

    static def routeLogcat(GenymotionVirtualDevice device, path){
        routeLogcatDevice(device.name, path)
    }

    static def routeLogcat(def deviceName, def path){
        cmd([GENYTOOL, DEVICE, deviceName, LOGCAT, path]){line, count ->
        }
        //TODO Check the request's feedback
    }



    /*
    TOOLS
     */

    /**
     * Fire a command line and process the result.
     * This function runs a closure for each line returned by the prompt.
     * The closure contains the parameters:
     * - <b>line</b> (containing the line's text)
     * - <b>count</b> (index of the line)
     *
     * @param command the command line to execute. It can be a String or a table
     * @param verbose true if you want to print each line returned by the prompt
     * @param c the closure to implement after the call
     */
    static def cmd(def command, boolean verbose=true, Closure c){

        def toExec = command

        //we eventually insert the genymotion binary path
        if(GENYMOTION_CONFIG != null && GENYMOTION_CONFIG.genymotionPath != null){
            if(toExec instanceof String){
                toExec = GENYMOTION_CONFIG.genymotionPath + toExec
            } else {
                toExec = command.clone()
                toExec[0] = GENYMOTION_CONFIG.genymotionPath + toExec[0]
            }
        }

        if(verbose) {
            println toExec
        }
        Process p = toExec.execute()
        StringBuffer error = new StringBuffer()
        StringBuffer out = new StringBuffer()
        p.consumeProcessOutput(out, error)

        p.waitForOrKill(GENYMOTION_CONFIG.processTimeout)

        if(verbose){
            println "error:" + error.toString()
            println "out:" + out.toString()
        }

        out.eachLine {line, count ->
            c(line, count)
        }

        return p.exitValue()
    }

    /**
     * Avoid null.toString returning "null"
     *
     * @param c the code to execute
     * @return the c's return
     */
    static def noNull(Closure c){
        //set null.toString to return ""
        String nullLabel = null.toString()
        NullObject.metaClass.toString = {return ''}

        def exit = c()

        //set as defaut
        NullObject.metaClass.toString = {return nullLabel}

        return exit
    }


}