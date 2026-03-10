import mill._
import scalalib._

import $file.ext.SpinalHDL.build
import ext.SpinalHDL.build.{core => spinalCore}
import ext.SpinalHDL.build.{lib => spinalLib}
import ext.SpinalHDL.build.{idslplugin => spinalIdslplugin}

val spinalVers = "1.13.0"
val spinalScalaVers = "2.13.12"
val scalaVers = "2.13.15"

object futurecore extends SbtModule {
  override def scalaVersion = scalaVers
  override def millSourcePath = os.pwd
  override def sources = T.sources(
    millSourcePath / "hw" / "spinal"
  )
  // def ivyDeps = Agg(
  //   ivy"com.github.spinalhdl::spinalhdl-core:$spinalVersion",
  //   ivy"com.github.spinalhdl::spinalhdl-lib:$spinalVersion"
  // )
  // def scalacPluginIvyDeps = Agg(ivy"com.github.spinalhdl::spinalhdl-idsl-plugin:$spinalVersion")
  def idslplugin = spinalIdslplugin(spinalScalaVers)
  override def moduleDeps: Seq[JavaModule] = Seq(
    spinalCore(spinalScalaVers),
    spinalLib(spinalScalaVers),
    idslplugin
  )
  override def scalacOptions: Target[Seq[String]] = super.scalacOptions() ++ idslplugin.pluginOptions()
  object test extends SbtTests with TestModule.ScalaTest {
    def sources = T.sources(
      this.millSourcePath / "sim" / "spinal"
    )
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.2.14"
    )
  }
}
