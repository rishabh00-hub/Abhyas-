import org.gradle.api.tasks.wrapper.Wrapper

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "9.5.1"
    distributionType = Wrapper.DistributionType.ALL
}
