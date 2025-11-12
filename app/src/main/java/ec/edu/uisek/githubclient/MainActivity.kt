package ec.edu.uisek.githubclient

import android.content.Intent
import android.os.Bundle
import android.security.advancedprotection.AdvancedProtectionManager
import android.view.textclassifier.ConversationActions
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ec.edu.uisek.githubclient.databinding.ActivityMainBinding
import ec.edu.uisek.githubclient.models.Repo
import ec.edu.uisek.githubclient.services.GithubApiService
import ec.edu.uisek.githubclient.services.RetrofitClient
import retrofit2.Response
import retrofit2.Call
import retrofit2.Callback
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var reposAdapter: ReposAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        binding.newRepoFab.setOnClickListener {
            displayNewRepoForm()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchRepositories()
    }

    private fun setupRecyclerView() {
        reposAdapter = ReposAdapter(
            onEditClick = { repo -> editRepository(repo) },
            onDeleteClick = { repo -> deleteRepository(repo) }
        )
        binding.reposRecyclerView.adapter = reposAdapter
    }

    private fun editRepository(repo: Repo) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Editar repositorio")

        // Contenedor de campos
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val nameInput = android.widget.EditText(this)
        nameInput.hint = "Nombre del repositorio"
        nameInput.setText(repo.name)
        layout.addView(nameInput)

        val descInput = android.widget.EditText(this)
        descInput.hint = "Descripción"
        descInput.setText(repo.description ?: "")
        layout.addView(descInput)

        builder.setView(layout)

        builder.setPositiveButton("Guardar") { _, _ ->
            val nuevoNombre = nameInput.text.toString()
            val nuevaDescripcion = descInput.text.toString()

            if (nuevoNombre.isNotEmpty()) {
                updateRepository(repo, nuevoNombre, nuevaDescripcion)
            } else {
                showMessage("El nombre no puede estar vacío")
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun updateRepository(repo: Repo, newName: String, newDescription: String) {
        val apiService = RetrofitClient.githubApiService
        val body = mapOf(
            "name" to newName,
            "description" to newDescription
        )

        val call = apiService.updateRepo(repo.owner.login, repo.name, body)
        call.enqueue(object : retrofit2.Callback<Repo> {
            override fun onResponse(call: retrofit2.Call<Repo>, response: retrofit2.Response<Repo>) {
                if (response.isSuccessful) {
                    showMessage("Repositorio actualizado correctamente")
                    fetchRepositories()
                } else {
                    showMessage("Error al actualizar: ${response.code()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<Repo>, t: Throwable) {
                showMessage("Error: ${t.message}")
            }
        })
    }


    private fun deleteRepository(repo: Repo) {
        val apiService = RetrofitClient.githubApiService
        val call = apiService.deleteRepo(repo.owner.login, repo.name)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    showMessage("Repositorio eliminado")
                    fetchRepositories()
                } else {
                    showMessage("Error al eliminar: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showMessage("Error: ${t.message}")
            }
        })
    }


    private fun fetchRepositories() {
        val apiService: GithubApiService = RetrofitClient.githubApiService
        val call = apiService.getRepos()

        call.enqueue(object: Callback<List<Repo>> {
            override fun onResponse(call: Call<List<Repo>?>, response: Response<List<Repo>?>) {
                if(response.isSuccessful) {
                    val repos = response.body()
                    if (repos != null && repos.isNotEmpty()) {
                        reposAdapter.updateRepositories(repos)
                    } else {
                        showMessage("No se encontraron repositorios")
                    }
                } else {
                    val errorMessage = when(response.code()) {
                        401 -> "No autorizado"
                        402 -> "Prohibido"
                        404 -> "No encontrado"
                        else -> "Error ${response.code()}"
                    }
                    showMessage("Error: $errorMessage")
                }
            }

            override fun onFailure(call: Call<List<Repo>?>, t: Throwable) {
                showMessage("No se pudieron cargar los repositorio")
            }
        })
    }

    private fun showMessage (message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun displayNewRepoForm() {
        Intent(this, RepoForm::class.java).apply {
            startActivity(this)
        }
    }
}