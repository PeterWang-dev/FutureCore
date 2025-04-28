import mill._, scalalib._

val spinalVersion = "1.12.0"

object futurecore extends SbtModule {
  def scalaVersion = "2.13.14"
  override def millSourcePath = os.pwd
  def sources = T.sources(
    millSourcePath / "hw" / "spinal"
  )
  def ivyDeps = Agg(
    ivy"com.github.spinalhdl::spinalhdl-core:$spinalVersion",
    ivy"com.github.spinalhdl::spinalhdl-lib:$spinalVersion"
  )
  def scalacPluginIvyDeps = Agg(ivy"com.github.spinalhdl::spinalhdl-idsl-plugin:$spinalVersion")
  object test extends SbtTests with TestModule.ScalaTest {
    def sources = T.sources(
      this.millSourcePath / "sim" / "spinal"
    )
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.2.14"
    )
  }
}
