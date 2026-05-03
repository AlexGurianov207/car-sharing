param(
    [string]$MavenGoal = "spring-boot:run"
)

$previousProfiles = $env:SPRING_PROFILES_ACTIVE

try {
    $env:SPRING_PROFILES_ACTIVE = "local"
    & ".\mvnw.cmd" $MavenGoal
    exit $LASTEXITCODE
} finally {
    $env:SPRING_PROFILES_ACTIVE = $previousProfiles
}
