# SpinalHDL Base Project for Ysyx

This repository is a base project to help Spinal users to **design their ysyx npc using Spinal HDL.**

## Things different from original project template

This project is a fork from [offical SpinalTemplateSbt](https://github.com/SpinalHDL/SpinalTemplateSbt).

**Based on the original template, this template for ysyx npc adds:**

* Add `Makefile` to introduce the GNU Make tool and some essential target that ysyx requires (such as tracer).
* Delete `build.sbt` and related files to completely remove sbt build tool as Makefile is made only for mill.
* Adjust names of objects to be consistent with ysyx project.

### TL;DR Things have already been set up in my environment, how do I run things to try SpinalHDL?

Once in the `YsyxSpinalTemplate` directory, when tools are installed, the commands below can be run to use `make`.

```sh
// To generate the Verilog from the example
make verilog

// To run the testbench
make test
```

* The example hardware description is into `hw/spinal/projectname/MyTopLevel.scala`
* The testbench is into `hw/spinal/projectname/MyTopLevelSim.scala`

Just like original template, you can still directly run `mill` commands to get Verilog, VHDL or start simulation (unit tests).However, this is strongly **not recommanded** , as elaboration and simulation will not be traced by this way. Just list commands here as reference.

```sh
// To generate the Verilog from the example
mill projectname.runMain projectname.Elaborate

// To run the testbench
mill projectname.runMain projectname.Simulate
```

## If you want to create a new project from this template

### Change project name

You might want to change the project name, which is currently `projectname`. To do so (let's say your actual project name is `myproject`; it must be all lowercase with no separators):

* Update `build.sc` by replacing `projectname` by the name of your project `myproject` .
* Update `Makefile` by replacing `projectname` by the name of your project `myproject` .
* Put all your scala files into `hw/spinal/myproject/` (remove the unused `hw/spinal/projectname/` folder) .
* Start all your scala files with `package myproject` .

### Change project structure

You can change the project structure as you want. The only restrictions (from Scala environment) are (let's say your actual project name is `myproject`):

* you must have a `myproject` folder and files in it must start with `package myproject` .
* if you have a file in a subfolder `myproject/somepackage/MyElement.scala` it must start with `package myproject.somepackage` .
* `mill` must be run right in the folder containing their configurations (recommended to not move these files).

Once the project structure is modified, update configurations:

* In `build.sc` (see above) replace `/ "hw" / "spinal"` by the new path to the folder containing the `myproject` folder.
* In the spinal configuration file (if you kept it, by default it is in `projectname/Config.scala` ), `targetDirectory = sys.env.getOrElse("TARGET_DIR", "hw/gen")` defines the location of generated verilog. **The `TARGET_DIR` is used in `Makefile` to cooperate with make variables ( `BUILD_DIR` ) and targets. You may change both `targetDirectory` in spinal configuration file and `BUILD_DIR` in `Makefile` .**
