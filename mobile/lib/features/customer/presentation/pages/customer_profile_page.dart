import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/ui/foodya_ui.dart';
import '../../../auth/presentation/cubit/login_cubit.dart';
import '../../domain/models/user_profile.dart';
import '../../domain/repositories/customer_profile_repository.dart';
import '../cubit/profile_cubit.dart';
import '../cubit/profile_state.dart';

class CustomerProfilePage extends StatelessWidget {
  const CustomerProfilePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create:
          (context) => ProfileCubit(
            repository: context.read<CustomerProfileRepository>(),
          )..load(),
      child: const _CustomerProfileView(),
    );
  }
}

class _CustomerProfileView extends StatefulWidget {
  const _CustomerProfileView();

  @override
  State<_CustomerProfileView> createState() => _CustomerProfileViewState();
}

class _CustomerProfileViewState extends State<_CustomerProfileView> {
  @override
  Widget build(BuildContext context) {
    return BlocConsumer<ProfileCubit, ProfileState>(
      listenWhen:
          (previous, current) =>
              previous.errorMessage != current.errorMessage ||
              previous.infoMessage != current.infoMessage,
      listener: (context, state) {
        final message = state.errorMessage ?? state.infoMessage;
        if (message != null) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(message)));
          context.read<ProfileCubit>().clearFeedback();
        }
      },
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Profile'),
            actions: [
              IconButton(
                icon: const Icon(Icons.edit_outlined),
                tooltip: 'Edit profile',
                onPressed:
                    state.isBusy || state.profile == null
                        ? null
                        : () => _showEditProfileDialog(context, state.profile!),
              ),
              IconButton(
                icon: const Icon(Icons.refresh_outlined),
                tooltip: 'Refresh',
                onPressed:
                    state.isBusy
                        ? null
                        : () => context.read<ProfileCubit>().load(),
              ),
            ],
          ),
          body:
              state.status == ProfileStatus.loading && state.profile == null
                  ? const Center(child: CircularProgressIndicator())
                  : RefreshIndicator(
                    onRefresh: () => context.read<ProfileCubit>().load(),
                    child: ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        _ProfileHeader(profile: state.profile),
                        const SizedBox(height: 20),
                        Card(
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Personal info',
                                  style: Theme.of(
                                    context,
                                  ).textTheme.labelLarge?.copyWith(
                                    color: Theme.of(context).colorScheme.primary,
                                  ),
                                ),
                                const SizedBox(height: 16),
                                _ProfileInfoTile(
                                  icon: Icons.person_outline,
                                  label: 'Full name',
                                  value: state.profile?.fullName ?? '—',
                                ),
                                const Divider(height: 28),
                                _ProfileInfoTile(
                                  icon: Icons.email_outlined,
                                  label: 'Email',
                                  value: state.profile?.email ?? '—',
                                ),
                                const Divider(height: 28),
                                _ProfileInfoTile(
                                  icon: Icons.phone_outlined,
                                  label: 'Phone number',
                                  value: state.profile?.phoneNumber ?? '—',
                                ),
                                const SizedBox(height: 16),
                                ImagePickerField(
                                  label: 'Profile photo',
                                  pickedFile: state.avatarImageFile,
                                  currentUrl: state.profile?.avatarUrl,
                                  onChanged: (file) {
                                    if (file != null) {
                                      context
                                          .read<ProfileCubit>()
                                          .uploadAvatar(file);
                                    }
                                  },
                                ),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        SizedBox(
                          width: double.infinity,
                          child: OutlinedButton.icon(
                            onPressed:
                                state.isBusy
                                    ? null
                                    : () => _showPasswordDialog(context),
                            icon: const Icon(Icons.lock_reset_outlined),
                            label: const Text('Change password'),
                          ),
                        ),
                        const SizedBox(height: 24),
                        SizedBox(
                          width: double.infinity,
                          child: OutlinedButton.icon(
                            style: OutlinedButton.styleFrom(
                              foregroundColor: Theme.of(context).colorScheme.error,
                              side: BorderSide(
                                color: Theme.of(context).colorScheme.error,
                              ),
                            ),
                            onPressed: () => _confirmLogout(context),
                            icon: const Icon(Icons.logout_outlined),
                            label: const Text('Log out'),
                          ),
                        ),
                      ],
                    ),
                  ),
        );
      },
    );
  }

  void _showEditProfileDialog(BuildContext context, UserProfile profile) {
    final formKey = GlobalKey<FormState>();
    final fullNameController = TextEditingController(text: profile.fullName);
    final emailController = TextEditingController(text: profile.email);
    final phoneController = TextEditingController(text: profile.phoneNumber);
    final cubit = context.read<ProfileCubit>();

    showDialog<void>(
      context: context,
      builder:
          (dialogContext) => AlertDialog(
            title: const Text('Edit profile'),
            content: Form(
              key: formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextFormField(
                    controller: fullNameController,
                    textCapitalization: TextCapitalization.words,
                    decoration: const InputDecoration(
                      labelText: 'Full name',
                      prefixIcon: Icon(Icons.person_outline),
                    ),
                    validator:
                        (value) =>
                            value == null || value.trim().isEmpty
                                ? 'Full name is required.'
                                : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: emailController,
                    decoration: const InputDecoration(
                      labelText: 'Email',
                      prefixIcon: Icon(Icons.email_outlined),
                    ),
                    keyboardType: TextInputType.emailAddress,
                    validator:
                        (value) =>
                            value == null || !value.contains('@')
                                ? 'Valid email is required.'
                                : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: phoneController,
                    decoration: const InputDecoration(
                      labelText: 'Phone number',
                      prefixIcon: Icon(Icons.phone_outlined),
                    ),
                    keyboardType: TextInputType.phone,
                    validator:
                        (value) =>
                            value == null || value.trim().isEmpty
                                ? 'Phone number is required.'
                                : null,
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () {
                  if (!formKey.currentState!.validate()) return;
                  Navigator.of(dialogContext).pop();
                  cubit.save(
                    fullName: fullNameController.text.trim(),
                    email: emailController.text.trim(),
                    phoneNumber: phoneController.text.trim(),
                    avatarUrl: profile.avatarUrl,
                  );
                },
                child: const Text('Save'),
              ),
            ],
          ),
    ).then((_) {
      fullNameController.dispose();
      emailController.dispose();
      phoneController.dispose();
    });
  }

  void _confirmLogout(BuildContext context) {
    final loginCubit = context.read<LoginCubit>();

    showDialog<void>(
      context: context,
      builder:
          (dialogContext) => AlertDialog(
            title: const Text('Log out?'),
            content: const Text(
              'You will need to sign in again to access your account.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Stay signed in'),
              ),
              FilledButton(
                style: FilledButton.styleFrom(
                  backgroundColor: Theme.of(dialogContext).colorScheme.error,
                  foregroundColor: Theme.of(dialogContext).colorScheme.onError,
                ),
                onPressed: () {
                  Navigator.of(dialogContext).pop();
                  loginCubit.logoutAll();
                },
                child: const Text('Log out'),
              ),
            ],
          ),
    );
  }

  void _showPasswordDialog(BuildContext context) {
    final currentController = TextEditingController();
    final nextController = TextEditingController();
    final confirmController = TextEditingController();
    final formKey = GlobalKey<FormState>();
    var showCurrent = false;
    var showNext = false;
    var showConfirm = false;

    showDialog<void>(
      context: context,
      builder:
          (dialogContext) => StatefulBuilder(
            builder:
                (_, setDialogState) => AlertDialog(
                  title: const Text('Change password'),
                  content: Form(
                    key: formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        TextFormField(
                          controller: currentController,
                          obscureText: !showCurrent,
                          decoration: InputDecoration(
                            labelText: 'Current password',
                            suffixIcon: IconButton(
                              icon: Icon(
                                showCurrent
                                    ? Icons.visibility_off_outlined
                                    : Icons.visibility_outlined,
                              ),
                              onPressed:
                                  () => setDialogState(
                                    () => showCurrent = !showCurrent,
                                  ),
                            ),
                          ),
                          validator:
                              (v) =>
                                  v == null || v.isEmpty ? 'Required.' : null,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: nextController,
                          obscureText: !showNext,
                          decoration: InputDecoration(
                            labelText: 'New password',
                            suffixIcon: IconButton(
                              icon: Icon(
                                showNext
                                    ? Icons.visibility_off_outlined
                                    : Icons.visibility_outlined,
                              ),
                              onPressed:
                                  () => setDialogState(
                                    () => showNext = !showNext,
                                  ),
                            ),
                          ),
                          validator:
                              (v) =>
                                  v == null || v.length < 6
                                      ? 'At least 6 characters.'
                                      : null,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: confirmController,
                          obscureText: !showConfirm,
                          decoration: InputDecoration(
                            labelText: 'Confirm password',
                            suffixIcon: IconButton(
                              icon: Icon(
                                showConfirm
                                    ? Icons.visibility_off_outlined
                                    : Icons.visibility_outlined,
                              ),
                              onPressed:
                                  () => setDialogState(
                                    () => showConfirm = !showConfirm,
                                  ),
                            ),
                          ),
                          validator:
                              (v) =>
                                  v != nextController.text
                                      ? 'Passwords do not match.'
                                      : null,
                        ),
                      ],
                    ),
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.of(dialogContext).pop(),
                      child: const Text('Cancel'),
                    ),
                    FilledButton(
                      onPressed: () {
                        if (!formKey.currentState!.validate()) return;
                        Navigator.of(dialogContext).pop();
                        context.read<ProfileCubit>().changePassword(
                          currentPassword: currentController.text,
                          newPassword: nextController.text,
                          confirmPassword: confirmController.text,
                        );
                      },
                      child: const Text('Update'),
                    ),
                  ],
                ),
          ),
    ).then((_) {
      currentController.dispose();
      nextController.dispose();
      confirmController.dispose();
    });
  }
}

class _ProfileHeader extends StatelessWidget {
  const _ProfileHeader({required this.profile});

  final UserProfile? profile;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final name = profile?.fullName ?? '';
    final initials = name
        .trim()
        .split(' ')
        .where((w) => w.isNotEmpty)
        .take(2)
        .map((w) => w[0].toUpperCase())
        .join();
    final avatarUrl = profile?.avatarUrl?.trim();
    final hasAvatar = avatarUrl != null && avatarUrl.isNotEmpty;

    return Column(
      children: [
        CircleAvatar(
          radius: 48,
          backgroundColor: theme.colorScheme.primaryContainer,
          backgroundImage:
              hasAvatar ? CachedNetworkImageProvider(avatarUrl) : null,
          child:
              hasAvatar
                  ? null
                  : Text(
                    initials.isEmpty ? '?' : initials,
                    style: theme.textTheme.headlineMedium?.copyWith(
                      color: theme.colorScheme.onPrimaryContainer,
                    ),
                  ),
        ),
        const SizedBox(height: 12),
        Text(
          name.isEmpty ? 'Your Profile' : name,
          style: theme.textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.w700,
          ),
          textAlign: TextAlign.center,
        ),
        if (profile != null) ...[
          const SizedBox(height: 4),
          Text(
            profile!.email,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          if (profile!.role.isNotEmpty) ...[
            const SizedBox(height: 8),
            Chip(
              label: Text(profile!.role),
              labelStyle: theme.textTheme.labelSmall,
              padding: EdgeInsets.zero,
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
          ],
        ],
      ],
    );
  }
}

class _ProfileInfoTile extends StatelessWidget {
  const _ProfileInfoTile({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, color: theme.colorScheme.onSurfaceVariant),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: theme.textTheme.labelMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 2),
              Text(value, style: theme.textTheme.bodyLarge),
            ],
          ),
        ),
      ],
    );
  }
}
