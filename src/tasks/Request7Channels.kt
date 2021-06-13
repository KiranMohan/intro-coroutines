package tasks

import contributors.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .bodyList()

        val channel = Channel<List<User>>()
        for ((index, repo) in repos.withIndex()) {
            launch {
                val users = service.getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                    .bodyList()

                channel.send(users)
            }

        }

        var allUsers = emptyList<User>()
        repeat(repos.size) {
            val users = channel.receive()
            allUsers = (allUsers + users).aggregate()
            updateResults(allUsers, it == repos.size)
        }
    }
}
